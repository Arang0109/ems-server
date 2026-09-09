# schedule 모듈 가이드라인

측정계획의 수립부터 현장 측정·실험실 분석·성적서 발행·주기 이행 이력까지를 다루는 모듈입니다.
`client_management` 다음으로 큰 모듈(157파일)이므로, 이 문서는 **탐색용 지도**와
**경계 판단의 근거**를 함께 담습니다.

> 이 모듈은 도메인 클래스 javadoc에 판단 근거가 촘촘히 적혀 있습니다. 이 문서는 그것을
> 되풀이하지 않고 **어디를 봐야 하는지 가리키는** 역할을 합니다.

---

## 애그리거트 지도

**4개 애그리거트, 2개 저장소(MySQL 2 · MongoDB 2).** 저장소가 애그리거트별로 갈립니다.

| 애그리거트 | 저장소 | 도메인 루트 | 담는 것 |
|---|---|---|---|
| **Schedule** (메타) | MySQL `schedules` | `domain/Schedule` | 상태·성적서 기본정보(관리번호·측정분야·측정용도·일자 4종)·연결키(stackId·teamId) |
| **ScheduleSnapshot** (세부) | MongoDB `schedule_documents` | `domain/snapshot/ScheduleSnapshot` | 측정 시점 원장 사본 + **측정 시트·실험분석정보 임베드** |
| **MeasurementRecord** (주기 이행 이력) | MySQL `measurement_records` | `domain/history/MeasurementRecord` | 완료 시점에 파생되는 확정 이력 |

```
Schedule (MySQL 메타 · 진실의 원천)
  └── ScheduleSnapshot (Mongo 세부)      1:1, scheduleId로 연결
        ├── ClientSnapshot → WorkplaceSnapshot → StackSnapshot → Facility/Prevention
        ├── TenantSnapshot                고객사 사본 + 성적서 서명란 담당자
        ├── TeamSnapshot                  측정자 + EquipmentSnapshot[]
        ├── SamplingSnapshot              채취시각·현장 담당자 + SamplingSheet[]
        └── SamplingItemSnapshot[]        측정항목 (성적서 항목 순서 = 배열 순서)
              └── AnalysisResult          실험분석정보 (null = 아직 분석 전)
  └── MeasurementRecord (MySQL)          1:N, 완료 시 파생
```

> **메타와 겹치는 값은 스냅샷에 두지 않습니다.** 상태와 성적서 기본정보 표의 값(관리번호·측정분야·
> 측정용도·채취일자·시료접수일·분석완료일·성적서발행일)은 전부 메타가 진실입니다. 2PC를 쓸 수 없는
> 구조에서 사본은 곧 "어느 쪽이 맞는가"라는 물음을 만들기 때문이며, 성적서 출력은 메타와 문서를
> 읽어 합칩니다(`ScheduleExportAssembler`).

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

`SamplingSheet`는 독립 애그리거트가 아니라 `ScheduleSnapshot`의 **하위 값 객체**입니다.

- `SamplingSnapshot.sheets` — 루트가 `sheets()` 위임 접근자로 대신 답합니다
- 물리적으로도 `ScheduleDocument`의 필드 — Mongo `schedule_documents` **한 문서 안**에 배열로
  들어갑니다. 시트만 담는 컬렉션이 없습니다
- 계산 입력이 전부 같은 스냅샷에서 나옵니다(`SnapshotSheetRecalculator`) —
  피토관 계수·노즐경·오리피스 보정계수는 `snapshot.equipments()`(= `team.equipments`)에서,
  표준산소농도·굴뚝 형상/치수는 `snapshot.client().workplace().stack()`에서 취합니다.
  **시트는 스냅샷 없이 계산될 수 없습니다**
- 역방향 의존도 있습니다 — `ScheduleProgress.hasMeasuredValue()`가 `SamplingPoint`의 `gasTemperature`·`dynamicPressure`·`staticPressure`를
  직접 읽어 `SCHEDULED → MEASURING` 전이를 판정합니다. 메타의 상태 머신이 시트 내부 필드에 의존합니다

**단, 계산 엔진은 이미 절연되어 있습니다.** `application/calculation/`의 13개 클래스는 스냅샷을 전혀
모르고 `StackData` DTO로만 소통합니다. `SnapshotSheetRecalculator`가 유일한 어댑터입니다.
훗날 분리 논의가 다시 나온다면 **여기가 유일하게 깨끗한 이음매**입니다.

### 2. Schedule과 ScheduleSnapshot은 한 개념의 두 저장소 표현

항상 `ScheduleDetail(meta, snapshot)` 쌍으로 다닙니다. MySQL과 MongoDB에 2PC를 걸 수 없어
**저장 순서로 정합성을 확보**하는 조율 로직이 `ScheduleService`와 `ScheduleStatusTransitioner`에 있습니다(아래 "이중 저장소 규약").
모듈을 가르면 이 조율이 모듈 경계를 넘어가 더 위험해집니다.

### 3. 실험분석정보는 측정항목 안으로 들어왔다

별도 애그리거트(`analysis_records` 컬렉션)였던 것을 `SamplingItemSnapshot.analysis`로 임베드했습니다.

- **판정 근거와 결과값의 사본이 하나가 됩니다.** `allowance`·`oxygenApplicable`이 항목과 분석 기록
  양쪽에 있어 한 회차 안에서 갈라질 수 있었고, 그래서 항목 정정 시 분석 기록을 따라 고치는
  동기화 경로(`syncJudgementBasis`)와 둘을 잇는 색인(`AnalysisRecordIndexer`)이 필요했습니다.
  임베드하면서 둘 다 사라졌습니다
- **분리의 근거였던 동시성은 상태 머신이 대신 막습니다.** 컬렉션을 나눈 이유는 실험실 입력이 시트
  저장의 문서 락과 부딪히지 않게 하려는 것이었는데, 실무에서 두 입력은 시간축에서 겹치지 않습니다
  (`ANALYZING`부터 기록지가 잠깁니다). 같은 단계에 열리는 실험·분석 탭과 성적서 탭은 쓰는 필드가
  겹치지 않아 논리 충돌이 없고, 물리 충돌은 `SnapshotWriter`가 재시도로 흡수합니다
- **생명주기도 단순해집니다.** `deleteSchedule()`이 measurementRecord → snapshot → meta 순으로
  지우면 되고, 문서 삭제가 곧 분석 결과 삭제입니다

history는 여전히 별개 애그리거트지만 같은 모듈에 둡니다 — 완료 시점에 파생되며
(`MeasurementRecordRecorder.recordCompletion`) 근거가 스냅샷 항목이라, 모듈을 가르면 `port/in`을
새로 만들고 트랜잭션 경계가 갈라지는데 얻는 것이 없습니다.

### 4. 모듈 경계는 이미 얇다

- **외부에서 이 모듈을 참조하는 것은 `ScheduleStatisticsUseCase` 하나**뿐입니다(`dashboard`가 사용).
  사실상 말단(leaf) 모듈이라 쪼개도 다른 모듈이 얻는 이득이 없습니다
- 반대로 이 모듈 → 외부는 `client_management`·`equipment`·`platform`의 `port/in`으로 나갑니다.
  진입점은 사실상 `ScheduleSnapshotAssembler` 한 곳입니다

---

## 이중 저장소 정합성 규약

MySQL(메타)과 MongoDB(세부)에 걸쳐 있고 **2PC를 쓸 수 없으므로**, 순서로 정합성을 확보합니다.

- **저장 순서: MySQL → Mongo.** 메타를 진실의 원천으로 두고, 문서 저장을 트랜잭션의 마지막
  부수효과로 배치합니다
- **삭제 순서: Mongo → MySQL** (저장의 역순).
  `deleteSchedule`은 measurementRecord → snapshot → meta 순입니다(문서 삭제가 곧 분석 결과 삭제)
- 상태 전이가 없는 문서 편집은 **문서 단독 쓰기**로 남습니다 — `saveAdvanced`가 상태가 실제로
  바뀔 때만 메타를 저장합니다

### 2단 낙관적 락

물리 충돌과 논리 충돌을 **다르게 처리해야 하므로** 층을 나눕니다. 물리 충돌은 재시도로 조용히 흡수하고,
논리 충돌만 사용자에게 알립니다.

| 층 | 토큰 | 막는 것 | 대응 |
|---|---|---|---|
| 문서 | `ScheduleDocument.@Version` (Spring Data) | 두 저장이 **물리적으로 겹친 것**. 논리 충돌이 아님 | 다시 읽어 재적용 (`SnapshotWriter.MAX_ATTEMPTS = 3`) |
| 시트 | `SamplingSheet.version` (서버 소유, 저장 시 +1) | **같은 시트를 먼저 저장한 것** | `SheetMerge`가 판정해 `SCHEDULE_SHEET_VERSION_CONFLICT` |

시트에는 식별자가 없고 `category`가 자연키이므로 충돌 판정 단위를 문서 전체가 아니라 **시트**로
잡습니다 — 두 사람이 서로 다른 기록지를 나눠 입력하는 흔한 경우에 충돌이 나지 않아야 합니다.

#### 문서 락을 공유하는 경로 7개

실험분석정보를 `items[].analysis`로 문서에 합치면서 아래 일곱이 같은 `@Version`을 놓고 경합합니다.
**문서를 쓰는 경로는 예외 없이 `SnapshotWriter`를 지납니다** — 어느 경로가 재시도를 타는지 사람이
외우게 두면 나중에 추가되는 경로에서 반드시 빠집니다.

| 경로 | 쓰는 필드 |
|---|---|
| `saveSheets` | `samplingData` 전체 — `sheets` + 채취시각 2·현장 담당자 2 |
| `saveAnalysisResults` | `items[].analysis` 의 실험실 입력 4필드 |
| `saveSamplingTimes` | `items[].analysis` 의 채취시각 2필드 |
| `changeItems` · `reorderItems` · `updateItem` | `items` 집합·순서·조건 |
| `changeEquipments` · `changeClient` | `team.equipments` · `client` 트리 + 재계산된 `sheets` |
| `changeTenant` · `changeTeam` | `tenant` 서명란 담당자 · `team` 측정자 표기 |

**변경 함수의 계약 3조**(정본은 `SnapshotWriter` javadoc) — ① 자기 소유 필드만 쓴다 ② 순수해야 한다
(재시도로 여러 번 호출되므로 이벤트 발행·MySQL 저장 금지) ③ 스냅샷과 무관한 검증은 루프 밖에서 끝낸다.

분석 결과에는 시트 `version` 같은 2층 토큰이 없습니다. 실험·분석 탭과 성적서 탭이 쓰는 필드가 겹치지
않아 논리 충돌이 성립하지 않기 때문이며, 그것이 두 저장 메서드가 나뉘어 있는 이유입니다
(`AnalysisResult.applyAnalysisResult` / `applySamplingTime`).

현장 입력과 실험실 입력은 상태 머신이 갈라 줍니다 — `ANALYZING`부터 기록지가 잠깁니다
(`Schedule.requireSheetEditable()`). 프론트의 잠금 설계를 서버가 실제로 보증하는 지점입니다.

### SSE는 커밋 이후에

`ScheduleSheetService`는 클래스 레벨 `@Transactional`이라 저장 직후 알리면 **뒤이어 롤백될 저장까지
"저장됐다"고 알리게** 됩니다. `publishAfterCommit`이 `TransactionSynchronization.afterCommit`으로
미룹니다. 알림 전송 실패가 저장을 되돌려서도 안 됩니다.

---

## 응답 계약 — 무엇이 진실인가

메타와 스냅샷에 같은 값이 있으면 **응답이 어느 쪽을 믿을지 클라이언트가 스스로 골라야 합니다.**
2PC가 없어 사본이 어긋날 수 있기 때문입니다. 그래서 **사본을 아예 두지 않는 쪽**으로 정리했습니다 —
상태와 성적서 기본정보(관리번호·측정분야·측정용도·일자 4종)는 메타에만 있고 문서에는 없습니다.

`ScheduleResponse`는 메타 11필드를 최상위에 펼치고, 세부는 `ScheduleSnapshotResponse`로 감쌉니다.
스냅샷 응답은 **문서의 저장 메타도 담지 않습니다** — `id`·`scheduleId`·`tenantId`는 최상위에 있고,
`version`(문서 단위 락)·`createdAt`(문서 생성 시각)은 서버 내부 값입니다.

| 남는 중복 | 이유 |
|---|---|
| `team.teamId`·`stack.stackId`·`tenant.tenantId` | 원장 연결키 |
| `stack.field` | 시설 자체의 측정분야. 계획의 `measurementField`와 가리키는 대상이 다릅니다 |
| `samplingData.sheets[].version` | **반드시 유지** — 클라이언트가 되돌려 보내야 시트 충돌을 판정합니다 |

> 성적서 서명란 담당자(`tenant.analyst`·`technicalManager`)와 측정자 표기(`team.mentorName`·
> `menteeName`)는 원장이 기본값을 갖지만 회차별로 다를 수 있어 스냅샷에서 덮어씁니다. 사본이 아니라
> **이 회차의 값**이므로 위 표의 중복에 해당하지 않습니다.

**도메인 타입을 그대로 노출하는 예외 3곳**입니다. 각각 기존 결정이 있어 뒤집지 않았습니다.

| 필드 | 근거 |
|---|---|
| `samplingData.sheets` (`SamplingSheet`) | `SaveSheetsRequest`가 도메인 시트를 그대로 입력받습니다. 시트는 읽어서 되돌려 보내는 **왕복 페이로드**라 응답만 감싸면 요청과 모양이 갈라집니다 |
| `team.equipments[].spec` (`EquipmentSpec`) | equipment 모듈의 **공유 커널** 조항이 직접 노출을 허용하며, 형제 응답 `EquipmentResponse`도 동일합니다. sealed 계층을 여기서만 다시 감쌀 이유가 없습니다 |
| `team.equipments[].inspections` (`InspectionItem`) | 위와 동일 조항 |

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

`application/service/` 직하에는 `@Service`만 두고, 협력자는 역할별 하위 패키지로 나눕니다
(루트 규칙 7의 협력자 배치 규약).

### 유스케이스 서비스 (`service/`)

측정계획 유스케이스는 **관심사 축으로 넷**입니다. 한 클래스가 21개 유스케이스를 들고 있던 것을
갈랐으며, CQRS 축(Command/Query)이 아닙니다 — 같은 애그리거트의 쓰기와 읽기는 함께 둡니다.

| 클래스 | 역할 |
|---|---|
| `ScheduleService` | 애그리거트 생명주기 — 생성·삭제·메타 수정(`PUT /{id}`·`PATCH /{id}/report-dates`)·상태 전이(완료·취소·재개방)·조회·목록 |
| `ScheduleSnapshotService` | 문서(스냅샷) 편집 7경로 — `changeEquipments`·`changeClient`·`changeTenant`·`changeTeam`·`changeItems`·`reorderItems`·`updateItem` |
| `ScheduleSheetService` | 측정 시트 저장(병합·재계산·SSE)과 채취 정보 저장, 이전 회차 불러오기 |
| `ScheduleStatisticsService` | `ScheduleStatisticsUseCase` 구현. **타 모듈(`dashboard`)에 여는 유일한 계약** |
| `AnalysisResultService` | 실험분석정보 유스케이스. 실험·분석 탭과 성적서 탭의 저장 경로를 분리 (결과는 `items[].analysis`에 저장) |
| `MeasurementHistoryService` | 이력 **조회만**. 쓰기는 완료 유스케이스에 종속된 부수효과이므로 한 서비스에 섞지 않음 |
| `ScheduleExportService` | jxls 템플릿 엑셀 내보내기(채취기록부 ZIP) |
| `ScheduleStreamService` | SSE 구독. 구독 전 tenant 소속 확인 — 없으면 id만 바꿔 타 고객사 편집 알림을 받을 수 있음 |

> `ScheduleController` 하나가 앞의 세 서비스를 주입받습니다. **엔드포인트가 곧 의도 선언**이라는
> 설계(아래 "수정 경로 규약")를 유지하려면 경로를 합치지 않는 것이 핵심이고, 어느 서비스에 사는지는
> 그다음 문제입니다.

### 조립 협력자 (`service/assembler/`)

| 클래스 | 역할 |
|---|---|
| `ScheduleSnapshotAssembler` | 측정 시점 스냅샷 조립. `client_management`·`equipment`·`platform` 포트를 모으는 **유일한 크로스모듈 허브** |
| `ScheduleExportAssembler` | 메타(MySQL)와 문서(Mongo)를 읽어 성적서 뷰로 합침 |
| `FulfillmentBoardDetailAssembler` | 주기 이행 현황판 조립. 행 축(측정항목)은 원장에서, 셀 값(이행 사실)은 이력에서. 조회 **2회 고정** |

### 정책 협력자 (`service/support/`)

| 클래스 | 역할 |
|---|---|
| `ScheduleStatusTransitioner` | 상태 전이 저장과 이력 동기화. **경로별 문서 저장 시점**을 한곳에 모음 (아래 참고) |
| `SnapshotWriter` | 문서 단위 낙관적 락 아래의 부분 갱신. 문서를 쓰는 **모든 경로가 여기를 지납니다** |
| `SnapshotSheetRecalculator` | 스냅샷에서 계산 입력(장비 spec·굴뚝 정보)을 뽑아 시트 재계산. 계산 엔진과 스냅샷 사이의 **유일한 어댑터** |
| `PreviousSheetFinder` | 새 기록지를 채울 이전 회차 시트 탐색. 직전 회차만 보지 않고 그 기록지를 실제로 쓴 회차를 거슬러 찾음(깊이 제한 `MAX_LOOKBACK`) |
| `MeasurementRecordRecorder` | 완료 시 이행 이력 기록 / 재개방·취소·삭제 시 해제 |

### 계산 엔진 (`application/calculation/`)

시트 계산 파이프라인. `SheetStep` 9개가 `@Order`로 실행:
Init(1) → Pressure(2) → Moisture(3) → ExhaustGas(4) → Density(5) → Flow(6) → Quantity(7) → Particle(8) → ApplyResult(999).
입력 DTO `StackData`도 여기 있습니다 — Command도 조회 VO도 아니어서 `command/`에 두지 않습니다.

### 상태 전이의 문서 저장 시점

`ScheduleStatusTransitioner`가 두 진입점으로 나뉘며, **차이는 문서를 언제 쓰느냐**입니다.
상태는 메타에만 있어 문서에 되비출 것이 없으므로, 이유 없는 문서 쓰기는 낙관적 락만 건드립니다.

| 메서드 | 문서 저장 | 쓰는 곳 |
|---|---|---|
| `confirmTransition` | **안 씀** (읽기만) | 사용자 확정 전이 — `complete`·`cancel`·`reopen` |
| `advanceAfterDocumentSaved` | **이미 저장됨** (`SnapshotWriter`가 씀) | 자동 전이 — 시트 저장·스냅샷 편집 6경로 전부 |

완료 훅(`syncMeasurementRecords`)은 `confirmTransition` 안에만 있습니다. 자동 전이는 분석값 입력
중까지만 전진시켜 성적서 작성 완료 경계를 넘지 않기 때문입니다.
저장 시점은 `ScheduleStatusTransitionerTest`가 회귀로 고정합니다.

### Validator (`application/validator/`)

| Validator | 메서드 |
|---|---|
| `ScheduleValidator` | `requireUniqueSchedule(tenantId, stackId, teamId, sampledAt)`, `requireExactItemOrder(items, orderedPollutantIds)` |

시트 전용 validator는 없습니다 — `SheetMerge`가 버전 충돌 판정을 겸합니다.
실험분석정보 validator도 없습니다 — 항목 유일성은 `items[]` 구조가 보장하고, 요청 내부 중복 검사는
포트 조회가 필요 없어 규칙 10에 따라 서비스 private으로 둡니다.

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
| `Schedule.updateMetadata` 의 측정용도·관리번호 | 측정정보 탭이 **단독 소유**해 폼이 자기 필드 전부를 보냄. 빈 칸은 "지웠다" |
| `Schedule.applyReportProgress` 의 일자 3종 | 실험·분석 탭이 **단독 소유**. 경로를 쪼개면서 성립했고, 그래서 잘못 넣은 일자를 비울 수 있음 |
| `SamplingItemSnapshot.allowance` · `oxygenApplicable` | 한번 채운 뒤 잘못 넣은 기준을 비울 방법이 없어짐 |
| `StackSnapshot.standardOxygen` | 위와 동일 |
| `AnalysisResult` 채취시간 (`applySamplingTime`) | 성적서 탭이 항목 표 **전체**를 보내는 일괄 저장이라 빈 칸은 "지웠다"는 뜻 |
| `AnalysisResult` 분석값 4필드 (`applyAnalysisResult`) | 실험·분석 탭도 동일 |

> `AnalysisResult`는 두 메서드 모두 전체 채택입니다 — 단건 부분 수정 경로를 두지 않기 때문입니다.
> 대신 **각자 자기 필드만** 건드립니다. 두 탭이 같은 문서를 쓰게 된 뒤에도 서로를 덮어쓰지 않는
> 근거가 이것이므로, 필드를 추가할 때 어느 탭 소유인지 먼저 정하세요.

**메타(`Schedule`)는 두 시맨틱이 메서드로 갈립니다.**

| 메서드 | 시맨틱 | 쓰는 경로 |
|---|---|---|
| `updateMetadata(sampledAt, schedulePurpose, referenceNumber)` | 전체 채택 (단 `sampledAt`은 null이면 유지 — DB NOT NULL이자 집계 기준일) | `PUT /{id}` |
| `applyReportProgress(receivedAt, analyzedAt, issuedAt)` | 전체 채택 + 순서 검증 | `PATCH /{id}/report-dates` |

> 한때 이 둘이 `update()` 하나였고 필드마다 시맨틱이 달랐습니다. 그 결과 자기 것이 아닌 칸에 null을
> 실어 보낸 호출자가 남의 값을 지웠습니다 — **한 메서드에 두 시맨틱을 섞지 마세요.**
>
> `applyReportProgress`의 순서 검증(`sampledAt ≤ received ≤ analyzed ≤ issued`)은 **빈 칸을 건너뛰되
> 사슬을 끊지 않습니다.** 전체 채택이라 중간 칸이 비어 올 수 있는데, 인접한 두 값만 견주면 접수일 없이
> 발행일만 넣었을 때 채취일과의 순서를 놓칩니다.

---

## API

### `/api/schedules` — `ScheduleController`

`POST /` · `GET /` · `GET /canceled` · `GET /{id}` · `PUT /{id}` · `PATCH /{id}/report-dates` ·
`POST /{id}/completion` · `POST /{id}/cancellation` · `POST /{id}/reopen` · `DELETE /{id}` ·
`PATCH /{id}/client` · `PATCH /{id}/tenant` · `PATCH /{id}/team` · `PATCH /{id}/equipments` ·
`PATCH /{id}/items` · `PUT /{id}/items/order` · `PATCH /{id}/items/{pollutantId}` · `PUT /{id}/sheets` ·
`GET /{id}/sheets/{category}/previous` · `GET /{id}/sheets/{category}/previous/candidates`

**엑셀** (`ScheduleExportController`, multipart 템플릿 업로드)
`POST /{id}/sampling-records/export` (채취기록부 ZIP)

**SSE** (`ScheduleStreamController`) `GET /{id}/stream`

### `/api/schedules/{scheduleId}/analyses` — `AnalysisResultController`

`GET /` · `PUT /results` · `PUT /sampling-times`

> 단건 등록·수정·삭제 경로는 두지 않습니다. 두 탭 모두 항목 표 **전체**를 보내는 일괄 저장이 실제
> 사용 방식이고, "빈 칸 = 지움" 규약이 있어 행 삭제가 빈 값 저장과 같은 뜻이 됩니다. 분석 결과가
> 문서 안에 있어 대리키(`analysisId`)도 없습니다 — 식별 축은 `pollutantId`입니다.

### `/api/measurement-records` — `MeasurementHistoryController`

`GET /` · `GET /fulfillment` · `GET /pending`

---

## 수정 경로 규약 — API를 하나로 합치지 않는 이유

측정계획 수정 경로가 11개인 것은 **의도된 설계**입니다. "측정계획 수정 API 하나"로 통합하지 않습니다.

| 경로 | 서비스 메서드 | 시트 재계산 | 문서 락 | 부작용 |
|---|---|---|---|---|
| `PUT /{id}` | `updateMeta` | 안 함 | — | **계획을 정의하는 값**(채취일자·측정용도·관리번호). 메타만, 전체 채택 |
| `PATCH /{id}/report-dates` | `updateReportDates` | 안 함 | — | **진행하며 채우는 일자 3종**. 메타만, 전체 채택 + 순서 검증, 자동 상태 전이 판정 |
| `PATCH /{id}/equipments` | `changeEquipments` | **함** | ○ | 팀 스냅샷 장비 목록 전체 교체 |
| `PATCH /{id}/client` | `changeClient` | **함** | ○ | 의뢰기관→사업장→측정시설 트리 병합 |
| `PATCH /{id}/tenant` | `changeTenant` | 안 함 | ○ | 성적서 서명란 담당자(+ 고객사 원장 사본). 부분 갱신 |
| `PATCH /{id}/team` | `changeTeam` | 안 함 | ○ | 이 회차 측정자 표기. 부분 갱신, 장비 목록은 건드리지 않음 |
| `PATCH /{id}/items` | `changeItems` | 안 함 | ○ | 기존 항목은 측정 시점 값 유지, 신규만 원장에서 조립 |
| `PUT /{id}/items/order` | `reorderItems` | 안 함 | ○ | **성적서 항목 순서 결정** |
| `PATCH /{id}/items/{pollutantId}` | `updateItem` | 안 함 | ○ | 이 회차 항목의 판정 근거 정정 |
| `PUT /{id}/sheets` | `saveSheets` | **함** | ○ | 시트 병합 + **채취시각·현장 담당자** + SSE 발행. `ANALYZING`부터 잠김 |
| `PUT /{id}/analyses/results` | `saveAnalysisResults` | 안 함 | ○ | `items[].analysis`의 실험실 입력 4필드 |
| `PUT /{id}/analyses/sampling-times` | `saveSamplingTimes` | 안 함 | ○ | `items[].analysis`의 채취시각 2필드 |

**모든 경로가 원장(`client_management`·`equipment`·`platform`)을 변경하지 않습니다.**
그리고 모두 `requireEditable()`을 지납니다 — 완료·취소된 계획은 수정할 수 없습니다.
기록지 저장만 `requireSheetEditable()`을 하나 더 지납니다.

"문서 락" ○ 는 그 경로가 문서를 써서 `SnapshotWriter`를 지난다는 뜻입니다. 위 "문서 락을 공유하는 경로"
절을 함께 보세요 — 각 경로가 자기 소유 필드만 쓰는 것이 서로를 덮어쓰지 않는 근거입니다.

### 경로를 가르는 기준 — 소유 화면

한때 성적서를 진행하며 채우는 값 11개가 `PATCH /{id}/basic-info` **한 경로**로 저장됐습니다.
목적지가 넷(메타·채취·고객사·팀)이라 서버가 나눠 보냈는데, 그 대가로 **두 화면이 한 경로를 공유**했고
각 화면이 자기 것이 아닌 칸에 null을 실어 보냈습니다. 그래서 부분 갱신일 수밖에 없었고,
**이미 채운 값을 비울 방법이 없었습니다.**

지금은 **소유 화면 기준**으로 갈랐습니다. 각 경로가 단독 소유가 되면서 전체 채택이 성립합니다.

| 경로 | 필드 | 소유 화면 | null |
|---|---|---|---|
| `PUT /{id}` | 채취일자·측정용도·관리번호 | 측정정보 탭 | **전체 채택** |
| `PATCH /{id}/report-dates` | 접수·분석완료·발행일 | 실험·분석 탭 | **전체 채택** |
| `PUT /{id}/sheets` | 채취시각 2·현장 담당자 2 (+시트) | 현장 채취 탭 | 부분 갱신 |
| `PATCH /{id}/tenant` | 서명란 담당자 | 두 탭 **공유** | 부분 갱신 |
| `PATCH /{id}/team` | 측정자 표기 | 현장 채취 탭 | 부분 갱신 |

null 시맨틱을 가르는 것은 **소유 화면의 수**입니다. 단독 소유면 폼이 자기 필드 전부를 보내므로 빈 칸을
"지웠다"로 읽을 수 있지만, 공유 경로에서는 전체 채택으로 두면 한쪽이 저장할 때마다 다른 쪽 입력이
사라집니다. 서명란 담당자(`tenant`)만 여전히 두 탭이 공유해 부분 갱신으로 남습니다 —
비우기가 필요해지면 그것도 화면별로 쪼갭니다.

**채취 정보가 시트 저장에 합류한 이유**는 같은 노드(`samplingData`)에 살고 같은 화면이 소유하기
때문입니다. 나눠 보내면 저장 한 번이 여러 왕복이 되고 중간에 실패하면 화면 상태가 갈라집니다.
대신 **잠금 시점을 시트와 공유합니다** — `ANALYZING`부터는 채취시각·현장 담당자도 함께 잠깁니다
(`requireSheetEditable`). 현장에서 확정되는 사실이라는 점에서 일관되지만, `basic-info`가 그 시점에도
수정을 허용하던 것에 비하면 축소입니다.

**측정분야와 대상(측정시설·측정팀)은 어느 경로로도 바꿀 수 없습니다.** 생성 시점에만 정합니다 —
측정분야가 바뀌면 측정항목과 성적서 서식이 통째로 달라지고, 대상이 바뀌면 스냅샷 정합성이 무너집니다.

### 통합하지 않는 근거

1. **재계산 여부가 경로마다 다릅니다.** 계산 입력(장비 spec·굴뚝 정보)이 바뀌는 경로만
   재계산합니다. 통합하면 "요청에 무엇이 왔는지" 보고 분기해야 하고, 빠뜨리면 조용히 계산값이
   낡습니다. **지금은 엔드포인트 자체가 의도 선언**이라 분기가 없습니다.
2. **null 시맨틱이 경로마다 다릅니다**(위 표 참고). 한 요청 DTO에 두 시맨틱이 섞이면
   클라이언트가 필드별로 null의 뜻을 외워야 합니다.
3. **동시 편집이 실제 업무 방식입니다.** 한 기록지를 섹션별로 나눠 입력하고, 실험·분석 탭과
   성적서 탭이 필드를 나눠 소유합니다. 한 문서에 저장하게 된 뒤에도 **필드 소유가 겹치지 않는 것**이
   서로를 덮어쓰지 않는 근거이므로, 통합 PUT은 그 근거를 없앱니다 — 문서 전체를 보내는 요청은
   자기 것이 아닌 필드까지 싣기 때문입니다.
4. **부작용 범위가 다릅니다.** 메타만 쓰는 경로(`PUT /{id}`·`report-dates`)와 문서만 쓰는 경로가 갈립니다 — 문서를 쓰지 않는 경로가 낙관적 락을 건드리지 않는 것이 요점입니다.

경로 11개는 많아 보이지만 각각이 **서로 다른 재계산·null·동시성 규약**을 갖습니다.
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
| `MeasurementRecordRepository` | MySQL 이력 |
| `ScheduleEventBroadcaster` | SSE. **시그니처에 `SseEmitter`가 드러납니다** — 의도된 예외이며 근거는 포트 javadoc에 있음 |
| `SheetExcelRenderer` | jxls 템플릿 렌더링 |

### Inbound Port (`application/port/in/`)

`ScheduleStatisticsUseCase` — `countCompleted`, `countCompletedInMonth`, `monthlyCompletedCounts`.
`dashboard`가 유일한 소비자입니다. 반환 VO `MonthlyMeasurementSummary`도 같은 패키지에 둡니다 —
포트 시그니처에 등장하고 타 모듈이 소비하므로 계약의 일부입니다(루트 `ARCHITECTURE.md`의 판단 기준).

이 하나 말고는 **이 모듈이 외부에 여는 것이 없습니다.** 새 조회 VO를 만들 때 `dashboard`가 쓰지
않는다면 `port/in`이 아니라 `application/command/`입니다.

### JPA 연관관계 없음

`ScheduleEntity`·`MeasurementRecordEntity` 모두 `@ManyToOne`/`@OneToMany`가 **0개**입니다.
`tenantId`·`stackId`·`teamId`·`scheduleId` 전부 plain `Long` 컬럼입니다
(루트 `ARCHITECTURE.md`의 ID 참조 규칙).

### 커맨드 하위 패키지 (`application/command/`)

`update/`는 **수정 계열 커맨드**를 담습니다 — `Update`·`Change`·`Reorder`·`Save` 접두사가 모두
여기 삽니다. 기준은 접두사가 아니라 <b>방향</b>이며, 파일 두엇을 위해 `save/`를 새로 파지 않습니다.

`command/` 직하에는 파일을 두지 않습니다. 계산 파이프라인 입력 DTO(`StackData`)는 Command도
조회 VO도 아니므로 `application/calculation/`에 있습니다.

**이벤트는 `command/`가 아니라 `application/event/`입니다** — `SheetsSavedEvent`·`EditorRef`.
루트 규칙 2의 이벤트 패키지 규약이며, 한때 `command/event/`에 있어 `port/out/ScheduleEventBroadcaster`가
커맨드 패키지를 참조하고 있었습니다. 이벤트는 알림 페이로드이지 유스케이스 입력이 아닙니다.

### 내보내기 뷰 (`application/command/export/`)

`~ExportView` 15개는 **record가 아니라 `@Getter` 클래스**입니다 — jxls의 JEXL이 getter로 해석하기
때문입니다. 하위 뷰는 항상 non-null을 보장합니다.

---

## 향후 과제

모듈 분리를 하지 않기로 한 대신, 다음이 남아 있습니다.

### presentation 이중 구조

`presentation/analysis/`·`presentation/history/`는 애그리거트별로 그룹핑되어 있는데
Schedule 본체는 `presentation/{controller,request,response,mapper}/` flat입니다.
한 모듈에 두 구조가 공존합니다. `domain/`은 `snapshot`·`sampling`·`history`로 나뉘어 있어
계층 간 비대칭도 있습니다 — 특히 `presentation/analysis/`는 대응하는 도메인 패키지가
없어졌는데도(실험분석정보가 `snapshot` 안으로 들어옴) 그대로 남아 있습니다.

> `equipment.domain.spec.*` 하위 구체 타입 결합과 `platform.application.result.TenantSummary`
> 참조는 `docs/architecture-audit-2026-08-24.md`가 별건으로 추적 중입니다.
