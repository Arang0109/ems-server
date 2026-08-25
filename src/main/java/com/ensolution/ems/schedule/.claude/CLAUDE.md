# schedule 모듈 가이드라인

측정계획의 수립부터 현장 측정·실험실 분석·성적서 발행·주기 이행 이력까지를 다루는 모듈입니다.
`client_management`(166) 다음으로 큰 모듈(154파일)이므로, 이 문서는 **탐색용 지도**와
**경계 판단의 근거**를 함께 담습니다.

> 이 모듈은 도메인 클래스 javadoc에 판단 근거가 촘촘히 적혀 있습니다. 이 문서는 그것을
> 되풀이하지 않고 **어디를 봐야 하는지 가리키는** 역할을 합니다.

---

## 애그리거트 지도

**4개 애그리거트, 2개 저장소(MySQL 2 · MongoDB 2).** 저장소가 애그리거트별로 갈립니다.

| 애그리거트 | 저장소 | 도메인 루트 | 담는 것 |
|---|---|---|---|
| **Schedule** (메타) | MySQL `schedules` | `domain/Schedule` | 상태·측정일·참조번호·연결키(stackId·teamId) |
| **ScheduleSnapshot** (세부) | MongoDB `schedule_documents` | `domain/snapshot/ScheduleSnapshot` | 측정 시점 원장 사본 + **측정 시트(sheets) 임베드** |
| **AnalysisRecord** (실험분석정보) | MongoDB `analysis_records` | `domain/analysis/AnalysisRecord` | 측정항목 1개당 문서 1건 |
| **MeasurementRecord** (주기 이행 이력) | MySQL `measurement_records` | `domain/history/MeasurementRecord` | 완료 시점에 파생되는 확정 이력 |

```
Schedule (MySQL 메타 · 진실의 원천)
  └── ScheduleSnapshot (Mongo 세부)      1:1, scheduleId로 연결
        ├── ClientSnapshot → WorkplaceSnapshot → StackSnapshot → Facility/Prevention
        ├── TeamSnapshot · TenantSnapshot · EquipmentSnapshot[]
        ├── SamplingItemSnapshot[]        측정항목 (성적서 항목 순서 = 배열 순서)
        └── MeasurementSheet[]            측정 시트 ← 별도 컬렉션이 아니라 이 문서 안
  ├── AnalysisRecord (Mongo)             1:N, 측정항목당 1건
  └── MeasurementRecord (MySQL)          1:N, 완료 시 파생
```

### 스냅샷의 존재 이유

측정 시점의 대상·팀·장비·측정항목을 **복사해 불변으로** 보관합니다. 원장(`client_management`·
`equipment`·`platform`)이 나중에 바뀌어도 과거 회차의 성적서와 초과 판정이 흔들리지 않아야 하기
때문입니다. 그래서 스냅샷 수정 API는 **원장을 절대 건드리지 않습니다** — 원장까지 고쳐야 하면
호출자가 원장 API를 따로 호출합니다.

---

## 경계 판단 — 왜 이대로 두는가

이 모듈을 `schedule` / `sheet` / `analysis` 등으로 **쪼개지 않기로 한 판단의 근거**입니다.
같은 질문이 반복되지 않도록 남깁니다.

### 1. sheet는 애그리거트가 아니다 — 분리할 대상 자체가 없다

`MeasurementSheet`는 독립 애그리거트가 아니라 `ScheduleSnapshot`의 **필드**입니다.

- `ScheduleSnapshot`의 record 컴포넌트 `List<MeasurementSheet> sheets`
- 물리적으로도 `ScheduleDocument`의 필드 — Mongo `schedule_documents` **한 문서 안**에 배열로
  들어갑니다. 시트만 담는 컬렉션이 없습니다
- 계산 입력이 전부 같은 스냅샷에서 나옵니다(`SnapshotSheetRecalculator`) —
  피토관 계수·노즐경·오리피스 보정계수는 `snapshot.equipments()`에서,
  표준산소농도·굴뚝 형상/치수는 `snapshot.client().workplace().stack()`에서 취합니다.
  **시트는 스냅샷 없이 계산될 수 없습니다**
- 역방향 의존도 있습니다 — `ScheduleProgress.hasMeasuredValue()`가 `SamplingPoint`의 `ts/pv/ps`를
  직접 읽어 `SCHEDULED → MEASURING` 전이를 판정합니다. 메타의 상태 머신이 시트 내부 필드에 의존합니다

**단, 계산 엔진은 이미 절연되어 있습니다.** `application/calculation/`의 13개 클래스는 스냅샷을 전혀
모르고 `StackData` DTO로만 소통합니다. `SnapshotSheetRecalculator`가 유일한 어댑터입니다.
훗날 분리 논의가 다시 나온다면 **여기가 유일하게 깨끗한 이음매**입니다.

### 2. Schedule과 ScheduleSnapshot은 한 개념의 두 저장소 표현

항상 `ScheduleDetail(meta, snapshot)` 쌍으로 다닙니다. MySQL과 MongoDB에 2PC를 걸 수 없어
**저장 순서로 정합성을 확보**하는 조율 로직이 `ScheduleService`에 있습니다(아래 "이중 저장소 규약").
모듈을 가르면 이 조율이 모듈 경계를 넘어가 더 위험해집니다.

### 3. analysis·history는 이미 별개 애그리거트지만, 같은 바운디드 컨텍스트

분리 난이도만 보면 `analysis`는 거의 분리된 상태입니다 — sheet 참조 0건, 별도 컬렉션, 별도
controller/validator/adapter 보유, 공유 타입은 `SamplingItemSnapshot` 하나뿐입니다. 그럼에도
같은 모듈에 둡니다.

- **생명주기가 종속**됩니다. `ScheduleService.deleteSchedule()`이
  measurementRecord → analysisRecord → snapshot → meta 순으로 함께 지웁니다
- **history는 완료 시점에 파생**됩니다(`MeasurementRecordRecorder.recordCompletion`).
  근거가 스냅샷 항목 + 분석 결과라 두 애그리거트를 동시에 읽어야 합니다
- **analysis는 스냅샷 항목 정정을 따라가야** 합니다
  (`ScheduleService.syncAnalysisJudgementBasis` → `AnalysisRecord.syncJudgementBasis`)
- 지금은 이 결합이 한 트랜잭션 안의 직접 포트 호출입니다. 모듈을 가르면 `port/in`을 새로 만들고
  트랜잭션 경계가 갈라지는데, 얻는 것이 없습니다

### 4. 모듈 경계는 이미 얇다

- **외부에서 이 모듈을 참조하는 것은 `ScheduleStatisticsUseCase` 하나**뿐입니다(`dashboard`가 사용).
  사실상 말단(leaf) 모듈이라 쪼개도 다른 모듈이 얻는 이득이 없습니다
- 반대로 이 모듈 → 외부는 `client_management`·`equipment`·`platform`의 `port/in`으로 나갑니다.
  진입점은 사실상 `ScheduleSnapshotAssembler` 한 곳입니다

### 5. analysis가 시트와 컬렉션을 나눈 이유는 별개다

애그리거트 분리와 모듈 분리는 다른 결정입니다. `analysis`가 `schedule_documents`에 얹히지 않고
별도 컬렉션에 있는 이유 — 실험실 입력이 시트 저장의 **문서 단위 낙관적 락과 부딪히지 않게** 하려는
것입니다. 근거는 `AnalysisRecord`·`AnalysisRecordDocument` 클래스 javadoc에 있습니다.

---

## 이중 저장소 정합성 규약

MySQL(메타)과 MongoDB(세부)에 걸쳐 있고 **2PC를 쓸 수 없으므로**, 순서로 정합성을 확보합니다.

- **저장 순서: MySQL → Mongo.** 메타를 진실의 원천으로 두고, 문서 저장을 트랜잭션의 마지막
  부수효과로 배치합니다
- **삭제 순서: Mongo → MySQL** (저장의 역순).
  `deleteSchedule`은 measurementRecord → analysisRecord → snapshot → meta 순입니다
- 상태 전이가 없는 문서 편집은 **문서 단독 쓰기**로 남습니다 — `saveAdvanced`가 상태가 실제로
  바뀔 때만 메타를 저장합니다

### 2단 낙관적 락

물리 충돌과 논리 충돌을 **다르게 처리해야 하므로** 층을 나눕니다. 물리 충돌은 재시도로 조용히 흡수하고,
논리 충돌만 사용자에게 알립니다.

| 층 | 토큰 | 막는 것 | 대응 |
|---|---|---|---|
| 문서 | `ScheduleDocument.@Version` (Spring Data) | 두 저장이 **물리적으로 겹친 것**. 논리 충돌이 아님 | 다시 읽어 병합 재시도 (`MAX_SHEET_SAVE_ATTEMPTS = 3`) |
| 시트 | `MeasurementSheet.version` (서버 소유, 저장 시 +1) | **같은 시트를 먼저 저장한 것** | `SheetMerge`가 판정해 `SCHEDULE_SHEET_VERSION_CONFLICT` |

시트에는 식별자가 없고 `category`가 자연키이므로 충돌 판정 단위를 문서 전체가 아니라 **시트**로
잡습니다 — 두 사람이 서로 다른 기록지를 나눠 입력하는 흔한 경우에 충돌이 나지 않아야 합니다.

### SSE는 커밋 이후에

`ScheduleService`는 클래스 레벨 `@Transactional`이라 저장 직후 알리면 **뒤이어 롤백될 저장까지
"저장됐다"고 알리게** 됩니다. `publishAfterCommit`이 `TransactionSynchronization.afterCommit`으로
미룹니다. 알림 전송 실패가 저장을 되돌려서도 안 됩니다.

---

## 응답 계약 — 무엇이 진실인가

메타와 스냅샷에 같은 값이 있을 때 **응답이 신뢰하는 쪽은 메타(MySQL)** 입니다. 2PC가 없어 문서 쪽
사본이 어긋날 수 있으므로, 사본을 함께 내보내면 클라이언트가 어느 쪽을 믿을지 스스로 골라야 합니다.

`ScheduleResponse`는 메타 11필드를 최상위에 펼치고, 세부는 `ScheduleSnapshotResponse`로 감쌉니다.
스냅샷 응답은 **문서의 저장 메타를 담지 않습니다** — `id`·`scheduleId`·`tenantId`·`status`는 최상위에
있고, `version`(문서 단위 락)·`createdAt`(문서 생성 시각)은 서버 내부 값입니다.

| 남는 중복 | 이유 |
|---|---|
| `basicInfo`의 관리번호·채취일자·측정분야·측정용도 | 최상위와 값은 같지만 **성적서 기본정보 표의 칸**입니다. 서버가 `BasicInfo.applyMeta`로 동기화하며, 어긋나면 최상위가 진실입니다 |
| `team.teamId`·`stack.stackId`·`tenant.tenantId` | 원장 연결키 |
| `stack.field` | 시설 자체의 측정분야. 계획의 `measurementField`와 가리키는 대상이 다릅니다 |
| `sheets[].version` | **반드시 유지** — 클라이언트가 되돌려 보내야 시트 충돌을 판정합니다 |

**도메인 타입을 그대로 노출하는 예외 3곳**입니다. 각각 기존 결정이 있어 뒤집지 않았습니다.

| 필드 | 근거 |
|---|---|
| `sheets` (`MeasurementSheet`) | `SaveSheetsRequest`가 도메인 시트를 그대로 입력받습니다. 시트는 읽어서 되돌려 보내는 **왕복 페이로드**라 응답만 감싸면 요청과 모양이 갈라집니다 |
| `equipments[].spec` (`EquipmentSpec`) | equipment 모듈의 **공유 커널** 조항이 직접 노출을 허용하며, 형제 응답 `EquipmentResponse`도 동일합니다. sealed 계층을 여기서만 다시 감쌀 이유가 없습니다 |
| `equipments[].inspections` (`InspectionItem`) | 위와 동일 조항 |

하위 트리 변환 메서드는 `ScheduleMapper.toSnapshotResponse` 하나만 선언하면 MapStruct가 이름 기준으로
전부 생성합니다. 스냅샷의 미매핑 필드는 `unmappedSourcePolicy = IGNORE`(기본값)로 조용히 빠지며,
**이것이 의도한 동작**입니다 — 응답 record에 자리를 만들지 않는 것이 곧 제외 선언입니다.

---

## 상태 전이

```
SCHEDULED ──► MEASURING ──► ANALYZING ──► REPORT_COMPLETED
    └──────────────┴──────────────┴──────► CANCELED  (어느 단계에서든)

REPORT_COMPLETED · CANCELED ──reopen──► 스냅샷에서 재도출한 단계
```

전이 규칙은 `ScheduleStatus.canTransitionTo()`가 소유합니다. 종단 상태(`REPORT_COMPLETED`·`CANCELED`)는
편집이 잠기고(`Schedule.requireEditable()`), 삭제는 실측 데이터가 없는 `SCHEDULED`·`CANCELED`에서만
가능합니다(`requireDeletable()`).

### 사용자 확정 전이 vs 자동 전이

| 구분 | 경로 | 무엇 |
|---|---|---|
| **사용자 확정** | `applyStatusChange` | `complete` · `cancel` · `reopen` |
| **자동** | `ScheduleProgress.advance` → `saveAdvanced` | 실측값(`ts`/`pv`/`ps`) 또는 채취 시작시각 입력 → `MEASURING`, 시료접수일 입력 → `ANALYZING` |

- 자동 전이는 `ANALYZING`까지만 전진시킵니다 — **성적서 작성 완료 경계를 넘지 않습니다.**
  그래서 완료 훅(`syncMeasurementRecords`)을 `applyStatusChange` **한 곳**에만 두면 충분합니다
- `reopen`이 돌아갈 단계는 스냅샷에서 재도출합니다. 취소는 세 단계 어디서든 걸 수 있어 되돌릴
  지점이 하나로 정해지지 않는데, 진행 단계가 원래 스냅샷에서 파생되는 값이라 같은 답이 나옵니다
- 완료·재개방은 이행 이력에 연동됩니다 — 완료로 확정되면 항목별 이행을 남기고, 완료가 풀리면
  그 계획이 만든 이행을 되돌립니다

---

## 서비스·컴포넌트 지도

`application/service/`는 flat이지만 역할은 애그리거트별로 갈립니다.

| 클래스 | 역할 |
|---|---|
| `ScheduleService` | 측정계획 유스케이스 전반. `ScheduleStatisticsUseCase` 구현. **632줄로 비대함 — 아래 "향후 과제" 참고** |
| `ScheduleSnapshotAssembler` | 측정 시점 스냅샷 조립. `client_management`·`equipment`·`platform` 포트를 모으는 **유일한 크로스모듈 허브** |
| `SnapshotSheetRecalculator` | 스냅샷에서 계산 입력(장비 spec·굴뚝 정보)을 뽑아 시트 재계산. 계산 엔진과 스냅샷 사이의 **유일한 어댑터** |
| `PreviousSheetFinder` | 새 기록지를 채울 이전 회차 시트 탐색. 직전 회차만 보지 않고 그 기록지를 실제로 쓴 회차를 거슬러 찾음(깊이 제한 `MAX_LOOKBACK`) |
| `MeasurementRecordRecorder` | 완료 시 이행 이력 기록 / 재개방·취소·삭제 시 해제. 이력 쓰기를 `ScheduleService`에서 분리 |
| `MeasurementHistoryService` | 이력 **조회만**. 쓰기는 완료 유스케이스에 종속된 부수효과이므로 한 서비스에 섞지 않음 |
| `FulfillmentBoardAssembler` | 주기 이행 현황판 조립. 행 축(측정항목)은 원장에서, 셀 값(이행 사실)은 이력에서. 조회 **2회 고정** |
| `AnalysisRecordService` | 실험분석정보 유스케이스. 실험·분석 탭과 성적서 탭의 저장 경로를 분리 |
| `AnalysisRecordIndexer` | 측정항목 ↔ 분석 결과 결합 규칙(`pollutantId` 축)을 한 곳에 모음. Recorder와 Export 양쪽이 사용 |
| `ScheduleExportService` / `ScheduleExportAssembler` | jxls 템플릿 엑셀 내보내기(성적서 단일 xlsx / 채취기록부 ZIP) |
| `ScheduleStreamService` | SSE 구독. 구독 전 tenant 소속 확인 — 없으면 id만 바꿔 타 고객사 편집 알림을 받을 수 있음 |
| `application/calculation/` | 시트 계산 파이프라인. `SheetStep` 9개가 `@Order`로 실행: Init(1) → Pressure(2) → Moisture(3) → ExhaustGas(4) → Density(5) → Flow(6) → Quantity(7) → Particle(8) → ApplyResult(999) |

### Validator (`application/validator/`)

| Validator | 메서드 |
|---|---|
| `ScheduleValidator` | `requireUniqueSchedule(tenantId, stackId, teamId, sampledAt)`, `requireExactItemOrder(items, orderedPollutantIds)` |
| `AnalysisRecordValidator` | `requireUniquePollutant(s)` |

시트 전용 validator는 없습니다 — `SheetMerge`가 버전 충돌 판정을 겸합니다.

---

## null 시맨틱 규약

**이 모듈은 null의 뜻이 경로마다 다릅니다.** 새 필드를 추가할 때 어느 쪽인지 먼저 정하세요.

| 시맨틱 | null·blank의 뜻 | 쓰이는 곳 | 헬퍼 |
|---|---|---|---|
| **부분 갱신** | 미전달 → 기존값 유지 | 대부분의 `update`·스냅샷 트리 병합 | `keep()` / `SnapshotMerge.keep`·`keepText` |
| **전체 채택** | 지움 | 일괄 저장 경로 | 없음 (전달값 그대로) |

**전체 채택인 예외 필드와 이유**

| 필드 | 이유 |
|---|---|
| `SamplingItemSnapshot.allowance` · `oxygenApplicable` | 한번 채운 뒤 잘못 넣은 기준을 비울 방법이 없어짐 |
| `StackSnapshot.standardOxygen` | 위와 동일 |
| `AnalysisRecord` 채취시간 (`applySamplingTime`) | 성적서 탭이 항목 표 **전체**를 보내는 일괄 저장이라 빈 칸은 "지웠다"는 뜻 |
| `AnalysisRecord` 분석값 4필드 (`applyAnalysisResult`) | 실험·분석 탭도 동일 |

> `AnalysisRecord.update()`(단건 수정)는 부분 갱신이고 `applyAnalysisResult()`(일괄 저장)는
> 전체 채택입니다. **같은 클래스 안에서 갈립니다** — 호출 경로를 확인하세요.

---

## API

### `/api/schedules` — `ScheduleController`

`POST /` · `GET /` · `GET /canceled` · `GET /{id}` · `PUT /{id}` · `PATCH /{id}/basic-info` ·
`POST /{id}/completion` · `POST /{id}/cancellation` · `POST /{id}/reopen` · `DELETE /{id}` ·
`PATCH /{id}/equipments` · `PATCH /{id}/client` · `PATCH /{id}/items` · `PUT /{id}/items/order` ·
`PATCH /{id}/items/{pollutantId}` · `PUT /{id}/sheets` ·
`GET /{id}/sheets/{category}/previous` · `GET /{id}/sheets/{category}/previous/candidates`

**엑셀** (`ScheduleExportController`, multipart 템플릿 업로드)
`POST /{id}/report/export` (성적서 단일 xlsx) · `POST /{id}/sampling-records/export` (채취기록부 ZIP)

**SSE** (`ScheduleStreamController`) `GET /{id}/stream`

### `/api/schedules/{scheduleId}/analyses` — `AnalysisRecordController`

`POST /` · `GET /` · `GET /{analysisId}` · `PUT /results` · `PUT /sampling-times` ·
`PUT /{analysisId}` · `DELETE /{analysisId}`

### `/api/measurement-records` — `MeasurementHistoryController`

`GET /` · `GET /fulfillment` · `GET /pending`

---

## 수정 경로 규약 — API를 하나로 합치지 않는 이유

측정계획 수정 경로가 8개인 것은 **의도된 설계**입니다. "측정계획 수정 API 하나"로 통합하지 않습니다.

| 경로 | 서비스 메서드 | 시트 재계산 | 부작용 |
|---|---|---|---|
| `PUT /{id}` | `updateSchedule` | 안 함 | 메타 + 문서 basicInfo 병합 |
| `PATCH /{id}/basic-info` | `updateBasicInfo` | 안 함 | 자동 상태 전이 판정 |
| `PATCH /{id}/equipments` | `changeEquipments` | **함** | 팀 스냅샷 장비 id·장비 목록 갱신 |
| `PATCH /{id}/client` | `changeClient` | **함** | 의뢰기관→사업장→측정시설 트리 병합 |
| `PATCH /{id}/items` | `changeItems` | 안 함 | 기존 항목은 측정 시점 값 유지, 신규만 원장에서 조립 |
| `PUT /{id}/items/order` | `reorderItems` | 안 함 | **성적서 항목 순서 결정** |
| `PATCH /{id}/items/{pollutantId}` | `updateItem` | 안 함 | **같은 항목의 실험분석정보 판정 근거 동기화** |
| `PUT /{id}/sheets` | `saveSheets` | **함** | 시트 병합 + SSE 발행 |

**모든 경로가 원장(`client_management`·`equipment`·`platform`)을 변경하지 않습니다.**
그리고 모두 `requireEditable()`을 지납니다 — 완료·취소된 계획은 수정할 수 없습니다.

### 통합하지 않는 근거

1. **재계산 여부가 경로마다 다릅니다.** 계산 입력(장비 spec·굴뚝 정보)이 바뀌는 경로만
   재계산합니다. 통합하면 "요청에 무엇이 왔는지" 보고 분기해야 하고, 빠뜨리면 조용히 계산값이
   낡습니다. **지금은 엔드포인트 자체가 의도 선언**이라 분기가 없습니다.
2. **null 시맨틱이 경로마다 다릅니다**(위 표 참고). 한 요청 DTO에 두 시맨틱이 섞이면
   클라이언트가 필드별로 null의 뜻을 외워야 합니다.
3. **동시 편집이 실제 업무 방식입니다.** 한 기록지를 섹션별로 나눠 입력하고, 실험·분석 탭과
   성적서 탭이 필드를 나눠 소유합니다. 통합 PUT은 문서 전체를 보내므로 서로의 입력을 덮어쓰고,
   시트 단위 낙관적 락도 문서 전체 단위로 후퇴합니다.
4. **부작용 범위가 다릅니다.** `updateItem`만 실험분석정보까지 건드립니다.

경로 8개는 많아 보이지만 각각이 **서로 다른 재계산·null·동시성 규약**을 갖습니다.
합치는 순간 그 차이가 전부 서비스 내부 조건문으로 이동합니다.

---

## 모듈 규칙

### tenant 소유권 격리

`client_management`의 규칙과 동일합니다 — tenantId는 `@AuthenticationPrincipal`에서만 얻고,
Port에 `(id, tenantId)`를 전달하며, 소유권 불일치는 404로 은닉합니다.
SSE 구독도 예외가 아닙니다(`ScheduleStreamService`).

### Outbound Port (`application/port/out/`)

| 포트 | 비고 |
|---|---|
| `ScheduleRepository` | MySQL 메타 |
| `ScheduleDocumentRepository` | Mongo 세부. `findByScheduleId(scheduleId, tenantId)` |
| `AnalysisRecordRepository` | Mongo. `deleteByScheduleId`로 계획 삭제 시 함께 정리 |
| `MeasurementRecordRepository` | MySQL 이력 |
| `ScheduleEventBroadcaster` | SSE. **시그니처에 `SseEmitter`가 드러납니다** — 의도된 예외이며 근거는 포트 javadoc에 있음 |
| `SheetExcelRenderer` | jxls 템플릿 렌더링 |

### Inbound Port (`application/port/in/`)

`ScheduleStatisticsUseCase` — `countCompleted`, `countCompletedInMonth`, `monthlyCompletedCounts`.
`dashboard`가 유일한 소비자입니다.

### JPA 연관관계 없음

`ScheduleEntity`·`MeasurementRecordEntity` 모두 `@ManyToOne`/`@OneToMany`가 **0개**입니다.
`tenantId`·`stackId`·`teamId`·`scheduleId` 전부 plain `Long` 컬럼입니다
(루트 `ARCHITECTURE.md`의 ID 참조 규칙).

### 내보내기 뷰 (`application/command/export/`)

`~ExportView` 15개는 **record가 아니라 `@Getter` 클래스**입니다 — jxls의 JEXL이 getter로 해석하기
때문입니다. 하위 뷰는 항상 non-null을 보장합니다.

---

## 향후 과제

모듈 분리를 하지 않기로 한 대신, 다음 두 가지가 남아 있습니다.

### `ScheduleService` 분해 (632줄)

유스케이스 14개에 analysis·이력 동기화 훅까지 한 클래스에 있습니다. 분해 축 후보:
상태 전이(`complete`/`cancel`/`reopen`/`applyStatusChange`) · 스냅샷 편집(`change*`/`update*`) ·
시트 저장(`saveSheets`/`mergeAndSaveSheets`) · 조회(`get*`·통계).
**모듈을 갈라도 이 문제는 그대로 남습니다** — 분리는 이 문제의 해법이 아닙니다.

### presentation 이중 구조

`presentation/analysis/`·`presentation/history/`는 애그리거트별로 그룹핑되어 있는데
Schedule 본체는 `presentation/{controller,request,response,mapper}/` flat입니다.
한 모듈에 두 구조가 공존합니다. `domain/`은 이미 `snapshot`·`sheet`·`analysis`·`history`로
나뉘어 있어 계층 간 비대칭도 있습니다.

> `equipment.domain.spec.*` 하위 구체 타입 결합과 `platform.application.result.TenantSummary`
> 참조는 `docs/architecture-audit-2026-08-24.md`가 별건으로 추적 중입니다.
