# client_management 모듈 가이드라인

측정대행 의뢰기관, 사업장, 측정시설(굴뚝)과 그 하위 설비·측정물질을 관리하는 모듈입니다.

## 도메인 모델 구조

```
Client (의뢰기관)
  └── Workplace (사업장)  1:N
        └── Stack (측정시설/굴뚝)  1:N
              ├── Facility (배출시설)  1:N
              └── Prevention (방지시설)  1:N
                    └── TargetSubstance (측정대상물질)  1:N

Pollutant (측정물질 마스터) ──< StackPollutant (시설별 측정물질) >── Stack
```

### 도메인 모델 책임
| 모델 | 책임 |
|------|------|
| `Client` | 측정대행을 의뢰하는 기관. 사업자번호 기준 식별 |
| `Workplace` | Client 산하의 개별 사업장. 자체 사업자번호 보유 |
| `Stack` | Workplace 산하의 개별 측정시설/굴뚝. 측정 분야·형태·등급 등 물리적 속성 포함 |
| `Facility` | Stack 산하의 배출시설. 연료 종류·사용량·투입량 관리 |
| `Prevention` | Stack 산하의 방지시설 |
| `TargetSubstance` | Prevention이 처리하는 측정대상물질. 제거효율 포함 |
| `Pollutant` | 측정물질 마스터 데이터(측정분야·상·측정방법 등). 특정 Stack에 종속되지 않음 |
| `StackPollutant` | Stack과 Pollutant를 연결하는 시설별 측정물질. 측정주기·허용치 관리 |

### Enum 위치
도메인 속성에서 사용하는 Enum(`Grade`, `MeasurementField`, `Shape`, `Orientation`,
`MeasurementMethod`, `MeasurementCycle`, `PollutantPhase` 등)은 `global/common/enums/`에서 관리합니다.

---

## 유스케이스 (Service)

도메인당 단일 `{도메인}Service`가 생성/조회/수정/삭제 유스케이스를 모두 담당합니다.
(Command/Query 서비스 분리는 채택하지 않습니다 — 루트 `CLAUDE.md`의 "Service 분리 방침" 참고.)

| 서비스 | 주요 메서드 |
|--------|------------|
| `ClientService` | `createClient`, `getClient`, `getClientList`, `updateClient`, `deleteClient` |
| `WorkplaceService` | `createWorkplace`, `getWorkplace`, `getWorkplaceList(clientId)`, `updateWorkplace`, `deleteWorkplace` |
| `StackService` | `createStack`, `getStackList(workplaceId)`, `getStackDetail(stackId)`, `updateStack`, `deleteStack` |
| `FacilityService` | `createFacility`, `getFacility`, `getFacilityList(stackId)`, `updateFacility`, `deleteFacility` |
| `PreventionService` | `createPrevention`, `getPrevention`, `getPreventionList(stackId)`, `updatePrevention`, `deletePrevention` |
| `TargetSubstanceService` | `createTargetSubstance`, `getTargetSubstance`, `getTargetSubstanceList(preventionId)`, `updateTargetSubstance`, `deleteTargetSubstance` |
| `PollutantService` | `createPollutant`, `getPollutant`, `getPollutantList(field)`, `updatePollutant`, `deletePollutant` |
| `StackPollutantService` | `createStackPollutant`, `getStackPollutantList(stackId)`, `removeStackPollutant` |

### 트리 조립 (Detail Assembler)
- `StackDetailAssembler` — Stack과 하위 aggregate(Facility·Prevention·TargetSubstance)를 각 Outbound Port로 읽어
  `StackDetail`로 조립합니다. 트리 조립 책임을 서비스·도메인 모델에서 분리한 `@Component`입니다.
- `StackService.getStackDetail()`이 이 Assembler에 위임합니다.

### 외부 공개 (Inbound Port)
- `WorkplaceService implements WorkplaceQueryUseCase` — 다른 모듈(contract 등)이 사업장 존재 확인·요약 조회 시
  사용하는 인바운드 계약. `application/port/in/`에 위치합니다.

---

## 향후 구현 예정

### 공통
- 페이징 (`Pageable`) 적용

---

## 모듈 규칙

### Repository Port (Outbound)
Outbound Port는 `application/port/out/`에 둡니다. (`domain/port/`가 아님 — 루트 `CLAUDE.md` 4번 규칙 참고.)

- `application/port/out/ClientRepository` — `save()`, `findById()`, `findAll()`, `existsByName()`, `deleteById()`
- `application/port/out/WorkplaceRepository` — `save()`, `findById()`, `findByClientId()`, `findAll()`, `existsByNameAndClientId()`, `existsById()`, `deleteById()`
- `application/port/out/StackRepository` — `save()`, `findById()`, `findByWorkplaceId()`, `findAll()`, `existsByNameAndWorkplaceIdAndField()`, `deleteById()`
- `application/port/out/FacilityRepository` — `save()`, `findById()`, `findByStackId()`, `deleteById()`
- `application/port/out/PreventionRepository` — `save()`, `findById()`, `findByStackId()`, `deleteById()`
- `application/port/out/TargetSubstanceRepository` — `save()`, `findById()`, `findByPreventionId()`, `deleteById()`
- `application/port/out/PollutantRepository` — `save()`, `findById()`, `findByField()`, `findAll()`, `existsByNameKr()`, `deleteById()`
- `application/port/out/StackPollutantRepository` — `save()`, `findByStackId()`, `existsByStackIdAndPollutantId()`, `deleteById()`
- 부모-자식 조회는 반드시 Port 메서드로 추상화합니다. (예: `findByClientId(Long clientId)`)
- `findById()` 는 `Optional` 을 반환하지 않고 `T` 를 직접 반환합니다. 없으면 Adapter에서 `NOT_FOUND` 예외를 던집니다.

### presentation 구조
- 애그리거트별 서브패키지로 그룹핑하고, 그 안에서 `controller/request/response/mapper`로 중첩합니다.
  - 예: `presentation/workplace/controller/WorkplaceController`, `presentation/workplace/mapper/WorkplaceMapper`

### 목록 조회 VO 패턴
- 목록 조회 시 연관 도메인 정보(부모명 등)를 포함하는 VO는 `application/command/list_item/`에 Record로 정의합니다.
  - `WorkplaceListItem` — `clientName` 포함 사업장 목록 아이템
  - `StackListItem` — `clientName`, `workplaceName` 포함 측정시설 목록 아이템
  - `StackPollutantListItem` — 시설별 측정물질 목록 아이템
- Infrastructure 매퍼(`{도메인}EntityMapper`)가 JPA 엔티티 → VO 변환을 담당합니다.
- Presentation 매퍼(`{도메인}Mapper`)가 VO → Response DTO 변환을 담당합니다.
  - 메서드명: `toListResponse()` / `toListResponses()`

### 연관 관계 조회
- 상위 도메인 조회 시 하위 aggregate를 즉시 로드하지 않습니다.
- 트리 형태의 상세가 필요하면 `StackDetailAssembler`처럼 각 Port를 조합해 상세 VO(`StackDetail`)로 조립합니다.
