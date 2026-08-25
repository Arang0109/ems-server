# equipment 모듈 가이드라인

측정 장비와 그 검사(정도검사·교정·일반시험) 이력을 관리하는 모듈입니다.

## 저장소
**MongoDB를 씁니다.** 장비 유형별 사양(`EquipmentSpec`)이 유형마다 구조가 달라 RDB 컬럼으로 펼치기 어렵기 때문입니다.
따라서 `docs/DATABASE.md`의 RDB 스키마에 `equipments` 테이블은 없습니다.

- 애그리거트 2종
  - `Equipment` → 컬렉션 `equipments`
  - `InspectionRecord` → 컬렉션 `equipment_inspection_records`
- `EquipmentSpec`은 sealed interface이며 Spring Data MongoDB가 `_class` 판별자로 구현체를 복원합니다.
  요청 DTO도 대칭 sealed 구조 + `@JsonTypeInfo(EXTERNAL_PROPERTY, property = "type")`입니다.
- Flyway/Liquibase가 없으므로 문서 구조를 바꿔도 **기존 문서는 자동으로 마이그레이션되지 않습니다.**
  구 필드는 도메인에 없으면 조용히 무시되고, `save()`가 문서 전체를 치환하므로 한 번 수정되면 자연 소멸합니다.

## 검사 체계 (이 모듈의 핵심 규칙)

측정장비가 받는 검사는 `InspectionType` 3종입니다 — `PRECISION_INSPECTION`(정도검사), `CALIBRATION`(교정), `GENERAL_TEST`(일반시험).

### 3종 고정 보유 + 플래그 토글
**세 종류는 서로 배타적이지 않습니다.** 한 장비가 정도검사와 교정을 동시에 받을 수 있고, 일반시험만 받는 장비도 있습니다.
그래서 목록에 항목을 넣고 빼는 대신, **모든 장비가 3종 항목을 항상 전부 보유**하고 플래그로 표현합니다.

| 플래그 | 의미 |
|---|---|
| `enabled` | 이 장비가 이 검사를 받는 대상인가. `false`면 예정일 계산도 알림 노출도 없다 |
| `notificationEnabled` | 임박 알림을 받을 것인가. `enabled`가 `true`일 때만 의미가 있다 |

두 플래그는 의미가 다릅니다 — 전자는 "그 검사를 받지 않는다", 후자는 "받긴 하지만 알림은 원하지 않는다"입니다.

이 구조 덕분에:
- 같은 종류가 중복 등록될 수 없어 중복 검증 자체가 필요 없습니다(`InspectionType`이 사실상 키).
- 수정 시 항목이 사라지면서 수검 이력까지 날아가는 사고가 구조적으로 불가능합니다.
- 검사 개념 도입 이전 문서(`inspections`가 없는 문서)도 `Equipment.getInspections()`의 정규화 한 곳에서 흡수됩니다.

`Equipment.getInspections()`는 Lombok `@Getter` 대신 **직접 정의**해 항상 3종을 enum 선언 순서대로 채워 반환합니다.
호출부에서 null·누락을 방어하지 마세요. 이 한 곳이 유일한 정규화 지점입니다.

### 다음 검사 예정일
`InspectionItem.nextDueDate()`가 소유하며 우선순위는 다음과 같습니다.

1. `enabled == false` → `null`
2. `nextDueDateOverride`(성적서에 적힌 유효기간 만료일)가 있으면 그 값
3. `lastInspectedAt` + `cycleMonths`개월
4. 위 값들을 특정할 수 없으면 `null` (임박 목록에서 제외)

`nextDueDateOverride`를 두는 이유: 정도검사증·교정성적서에는 유효기간 만료일이 직접 인쇄되며, 기관이 정하는 만료일이
"수검일 + N개월" 단순 가산과 어긋나는 경우가 흔합니다. 검사 실시 기록의 `validUntil`이 이 값을 채웁니다.

### PUT의 `inspections` 시맨틱 (가장 오해하기 쉬운 지점)
- **전달하지 않은 검사 종류는 손대지 않습니다.** 전체 치환이 아니라 종류별 부분 갱신입니다.
- 항목 하나 안에서도 `null` 필드는 미전달로 보아 기존 값을 유지합니다(도메인 `update()`의 keep 시맨틱).
- 그래서 플래그는 요청·`InspectionItemChange` 모두 **`Boolean`**입니다. `boolean`으로 받으면 미전달이 `false`가 되어
  전달하지 않은 검사가 조용히 꺼집니다.
- "검사 대상에서 뺀다"는 항목 삭제가 아니라 `enabled: false`입니다. 주기·수검일은 남으므로 다시 켜면 그대로 복구됩니다.

### 검사 실시 기록
`POST /api/equipments/{equipmentId}/inspections`가 이력(`InspectionRecord`)을 남기고 해당 항목의 `lastInspectedAt`을 갱신합니다.
장비 수정으로 날짜를 덮어쓰는 방식이 아닙니다.

- `InspectionRecord`는 **한 번 기록되면 변경되지 않으므로 `update()`를 두지 않습니다**(storage의 `DocumentVersion`과 동일).
- `EquipmentService.recordInspection()`은 **이력을 먼저 저장한 뒤** 장비를 갱신합니다. MongoDB standalone에는 문서 간
  트랜잭션이 없으므로, 중간 실패 시 "이력은 남고 수검일만 미반영"이어야 재입력 없이 복구할 수 있습니다.
  반대 순서라면 근거 이력 없이 수검일만 미뤄진 장비가 남습니다.
- 검사 대상이 아닌 종류는 기록할 수 없습니다(`EQUIPMENT_INSPECTION_DISABLED`). 이 검증은 **이력을 저장하기 전에** 호출해야
  거부된 요청의 이력만 남는 일이 없습니다.
- 성적서 파일 첨부는 아직 미구현입니다. MongoDB는 스키마리스라 추후 storage 모듈의 문서 id 필드를 추가하는 것만으로
  비파괴적으로 확장할 수 있으므로, 지금 쓰지 않는 필드를 API 계약에 미리 노출하지 않습니다.

### 유형별 기본 검사 세트 (`InspectionPolicy`)
장비 **등록 시점의 초기값만** 결정합니다. 이 정책을 고쳐도 **이미 등록된 장비에는 소급 적용하지 않습니다.**
기본으로 켤 검사만 선언하고, 나머지를 비활성으로 채우는 정규화는 `Equipment`가 담당합니다(정책과 정규화가 두 곳으로 갈리지 않도록).

> **미확정**: 유형별 기본 활성 검사와 기본 주기는 법령·기관 지침에 달린 도메인 지식이라 아직 비어 있습니다.
> 현재는 전 유형이 3종 모두 비활성으로 등록되고 사용자가 장비별로 켜서 씁니다. 값이 정해지면 `InspectionPolicy` 한 파일만 고치면 됩니다.

## Validator를 두지 않는 이유
루트 규칙 10은 **포트 조회가 필요한 규칙**만 Validator로 뽑으라고 합니다.
이 모듈에서 필요한 검증(비활성 검사에 이력 기록 금지)은 서비스가 이미 로드한 애그리거트의 내부 상태만 보면 되므로
도메인 불변식 `Equipment.requireInspectionEnabled()`가 맞습니다. Validator로 빼면 단순 위임 래퍼가 되어 규칙 10 단서에 위배됩니다.
종류 중복 검증은 3종 고정 구조라 애초에 불필요합니다.

## 타 모듈 공개 계약 (`application/port/in`)
- `EquipmentQueryUseCase.getEquipmentSummary(equipmentId, tenantId)` → `EquipmentSummary`
  - `schedule`의 `ScheduleSnapshotAssembler`가 측정 시점 스냅샷을 조립할 때 씁니다.
- `EquipmentQueryUseCase.findInspectionDueBefore(tenantId, dueDate)` → `List<InspectionDueSummary>`
  - **단위가 장비가 아니라 장비-검사항목입니다.** 한 장비가 여러 검사에서 임박하면 항목 수만큼 여러 건이 나옵니다.
  - 알림 대상(`notifiable()`)이면서 ACTIVE인 장비만, 예정일 오름차순. 기한 초과 항목도 포함합니다.
  - 잔여일수 같은 표시용 파생값은 담지 않습니다. 기준일을 아는 소비 모듈(`dashboard`)이 계산합니다.
### 공유 커널 — 도메인 타입 직접 노출

이 모듈은 도메인 타입을 `port/in`에 **그대로** 노출하며, 소비 모듈이 직접 import 하는 것을 허용합니다
(루트 `CLAUDE.md` 규칙 1의 공유 커널 조항). 포트 시그니처에 이미 드러난 타입을 소비 모듈마다 다시 감싸면
변환 계층만 늘고 얻는 것이 없기 때문입니다.

**공개 범위** — 아래 타입과 그 하위 구체 타입까지입니다. 여기 없는 타입은 노출 대상이 아닙니다.

| 타입 | 소비 모듈 | 용도 |
|---|---|---|
| `EquipType` | schedule | 스냅샷 장비 유형, 성적서 출력 |
| `InspectionType` | schedule, dashboard | 검사 종류 |
| `InspectionItem` | schedule | 스냅샷 검사 이력 |
| `EquipmentSpec` (sealed) | schedule | 스냅샷 장비 사양. Spring Data MongoDB가 `_class` 판별자로 구현체를 복원합니다 |
| `EquipmentSpec` 하위 구체 타입 — `NozzleSpec`·`PitotTubeSpec`·`ParticleSamplerSpec` 등 | schedule | 측정 시트 계산 입력(노즐경·피토관 계수·오리피스 보정계수). 계산식이 사양의 **구체 값**을 직접 읽어야 하므로 sealed 상위 타입만으로는 부족합니다 |

- **역방향 참조는 금지입니다** — 이 모듈이 `schedule`·`dashboard`를 알아서는 안 됩니다.
- `EquipmentSpec` 계층의 클래스명·패키지를 바꾸면 **Mongo `_class` 판별자 값이 바뀌어 기존 문서를 못 읽습니다.**
  변경 시 `docs/migration/`에 백필 스크립트가 필요합니다(루트 규칙 15).

## 엔드포인트
- `POST /api/equipments` 등록 — `inspections`를 생략하면 유형별 기본 세트가 주입됩니다.
- `GET /api/equipments?type=` 목록 / `GET /api/equipments/{equipmentId}` 상세
- `PUT /api/equipments/{equipmentId}` 수정 — 전달하지 않은 필드·검사 종류는 기존 값 유지
- `PATCH /api/equipments/{equipmentId}/status` 상태 변경 / `DELETE` 소프트 삭제(`EquipStatus.DELETED`)
- `POST /api/equipments/{equipmentId}/inspections` 검사 실시 기록
- `GET /api/equipments/{equipmentId}/inspections` 검사 이력 목록(최근 수검일 순)

## 규칙
- tenantId는 `@AuthenticationPrincipal CustomUserDetails`에서만 획득합니다(path/body 금지).
  미존재·타 tenant는 `EQUIPMENT_NOT_FOUND`로 은닉합니다.
- 응답은 `global/web/ApiResponse<T>`.
- 응답 DTO의 `nextDueDate`·`typeLabel`은 계산값입니다. record 접근자 메서드는 Jackson이 직렬화하지 않으므로 필드로 노출합니다.
- 삭제는 소프트 삭제이며, 모든 조회 경로가 `statusNot(DELETED)`로 거릅니다.

## 향후 과제
- `findInspectionDueBefore`가 `EquipmentRepository.findAll(tenantId)` 전건 로드 후 스트림 필터입니다.
  예정일이 저장 값이 아니라 계산 값이라 Mongo 쿼리로 직접 거를 수 없으므로, 장비가 늘면 예정일을 문서에 비정규화하거나
  aggregation 파이프라인으로 옮겨야 합니다.
- `schedule`의 `EquipmentExportView`는 기존 엑셀 템플릿 호환을 위해 `calibrationCycle`·`lastCalibrationDate`·`calibrationDueDate`를
  `CALIBRATION` 항목에서 파생해 유지합니다. 템플릿(.xlsx)은 저장소 밖에서 업로드되는 사용자 자산이라 서버 배포와 동시에
  고칠 수 없기 때문입니다. 운영 중인 템플릿을 전부 `inspections` 기반으로 교체한 뒤에야 제거할 수 있습니다.
