# client_management 모듈 가이드라인

측정대행 의뢰기관, 사업장, 측정시설(굴뚝)과 그 하위 설비·측정물질을 관리하는 모듈입니다.

## 도메인 모델 구조

```
Client (의뢰기관)
  └── Workplace (사업장)  1:N
        └── Stack (측정시설/굴뚝)  1:N
              ├── Facility (배출시설)  1:N
              └── Prevention (방지시설)  1:N

Pollutant (측정물질 마스터) ──< StackPollutant (시설별 측정물질) >── Stack

Team (측정 팀)  ── tenant 직속. 사수·부사수(auth users)·측정 장비(equipment) id 관리
```

### 도메인 모델 책임
| 모델 | 책임 |
|------|------|
| `Client` | 측정대행을 의뢰하는 기관. 사업자번호 기준 식별 |
| `Workplace` | Client 산하의 개별 사업장. 자체 사업자번호 보유 |
| `Stack` | Workplace 산하의 개별 측정시설/굴뚝. 측정 분야·형태·등급 등 물리적 속성 포함 |
| `Facility` | Stack 산하의 배출시설. 연료 종류·사용량·투입량 관리 |
| `Prevention` | Stack 산하의 방지시설. 처리 대상물질명(`targetName`)·제거효율(`removalEfficiency`)을 자체 필드로 보유 |
| `Pollutant` | 측정물질 마스터 데이터(측정분야·상·측정방법 등). 특정 Stack에 종속되지 않음 |
| `StackPollutant` | Stack과 Pollutant를 연결하는 시설별 측정물질. 측정주기·허용치 관리 |
| `Team` | tenant 직속 측정 팀. 사수(mentor)·부사수(mentee) user id와 장비 id 4종(입자샘플러·가스샘플러·피토관·노즐) 관리. users는 `auth`, 장비는 `equipment` 모듈 소유이므로 **plain id 컬럼**만 보관(FK 없음) |

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
| `PollutantService` | `createPollutant`, `getPollutant`, `getPollutantList(field)`, `updatePollutant`, `deletePollutant` |
| `StackPollutantService` | `createStackPollutant`, `getStackPollutantList(stackId)`, `removeStackPollutant` |
| `TeamService` | `createTeam`, `getTeam`, `getTeamList`, `updateTeam`, `deleteTeam`. 사수·부사수 user 검증은 `TeamValidator`에 위임(아래 참고). 장비 id는 검증 없이 저장 |

### 검증 (Validator)
비즈니스 규칙 검증은 `application/validator/`의 `{애그리거트}Validator`가 담당합니다.
서비스는 조건식 대신 `require*()` 호출만 남깁니다. (루트 `CLAUDE.md` 10번 규칙 — 이 모듈이 레퍼런스 구현입니다.)

| Validator | 메서드 | 규칙 |
|-----------|--------|------|
| `ClientValidator` | `requireUniqueName(name)` | 의뢰기관명 유일 |
| `WorkplaceValidator` | `requireUniqueNameInClient(name, clientId)` | 사업장명은 의뢰기관 내 유일 |
| `StackValidator` | `requireUniqueNameInWorkplace(name, workplaceId, field)` | 측정시설명은 사업장·측정분야 내 유일 |
| `PollutantValidator` | `requireUniqueNameKr(nameKr, tenantId)` | 측정물질 국문명은 tenant 내 유일 |
| `StackPollutantValidator` | `requireNotRegistered(stackId, pollutantId)` | 같은 시설에 같은 물질 중복 등록 불가 |
| | `requireNoDuplicatesInBatch(commands)` | 일괄 등록 요청 내부의 자기 중복 차단(부분 저장 방지) |
| `TeamValidator` | `requireUniqueName(name, tenantId)` | 팀명은 tenant 내 유일 |
| | `requireMemberInTenant(userId, tenantId, notFound)` | 사수·부사수 user가 존재하고 해당 tenant 소속인지 `auth`의 `UserQueryUseCase`로 확인. 미존재·타 tenant 모두 `TEAM_MENTOR_NOT_FOUND`/`TEAM_MENTEE_NOT_FOUND`로 은닉 |

- **Validator가 다루지 않는 것**: 단건 존재·소유권 검증은 그대로 Adapter의 `findById(id, tenantId)` + `NOT_FOUND`가 담당합니다(아래 "tenant 소유권 격리" 참고). 형식·필수값은 Request DTO의 Bean Validation이 담당합니다.
- Validator는 port/out과 타 모듈 port/in만 주입받고 `@Transactional`을 갖지 않습니다.
- 중복 체크 파라미터에 tenant를 넣는 기준은 아래 "tenant 소유권 격리"의 `existsBy...` 규칙과 동일합니다.

### 트리 조립 (Detail Assembler)
- `StackDetailAssembler` — Stack과 하위 aggregate(Facility·Prevention)를 각 Outbound Port로 읽어
  `StackDetail`로 조립합니다. 트리 조립 책임을 서비스·도메인 모델에서 분리한 `@Component`입니다.
- `StackService.getStackDetail()`이 이 Assembler에 위임합니다.
- `TeamAssembler` — Team 도메인에 타 모듈(`auth`)의 사수·부사수 **이름**을 결합해 `TeamDetail`/`TeamListItem`으로 조립합니다.
  users는 별도 모듈이라 JPA 조인 불가 → `UserQueryUseCase`(인바운드 포트)로 채웁니다. 목록은 `getUserList(tenantId)` 1회 + `Map`으로 N+1을 회피합니다.

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

- `application/port/out/ClientRepository` — `save()`, `findById(id, tenantId)`, `findAll(tenantId)`, `existsByName()`, `deleteById(id, tenantId)`
- `application/port/out/WorkplaceRepository` — `save()`, `findById(id, tenantId)`, `findByClientId(clientId, tenantId)`, `findAll(tenantId)`, `existsByNameAndClientId()`, `existsById()`, `deleteById(id, tenantId)`
- `application/port/out/StackRepository` — `save()`, `findById(id, tenantId)`, `findByWorkplaceId(workplaceId, tenantId)`, `findAll(tenantId)`, `findFieldsByWorkplaceIds(ids, tenantId)`, `existsByNameAndWorkplaceIdAndField()`, `deleteById(id, tenantId)`
- `application/port/out/FacilityRepository` — `save()`, `findById(id, tenantId)`, `findByStackId(stackId, tenantId)`, `deleteById(id, tenantId)`
- `application/port/out/PreventionRepository` — `save()`, `findById(id, tenantId)`, `findByStackId(stackId, tenantId)`, `deleteById(id, tenantId)`
- `application/port/out/PollutantRepository` — `save()`, `findById(id, tenantId)`, `findByField(field, tenantId)`, `findAll(tenantId)`, `existsByNameKrAndTenantId()`, `deleteById(id, tenantId)`
- `application/port/out/StackPollutantRepository` — `save()`, `findByStackId(stackId, tenantId)`, `existsByStackIdAndPollutantId()`, `deleteById(id, tenantId)`
- `application/port/out/TeamRepository` — `save()`, `findById(id, tenantId)`, `findAll(tenantId)`, `existsByNameAndTenantId()`, `deleteById(id, tenantId)`
- 부모-자식 조회는 반드시 Port 메서드로 추상화합니다. (예: `findByClientId(Long clientId, Long tenantId)`)
- `findById()` 는 `Optional` 을 반환하지 않고 `T` 를 직접 반환합니다. 없으면 Adapter에서 `NOT_FOUND` 예외를 던집니다.

### tenant 소유권 격리 (멀티테넌시)
모든 aggregate는 로그인 사용자의 tenant 범위 안에서만 조회/수정/삭제됩니다.

- tenantId는 **오직 `@AuthenticationPrincipal CustomUserDetails`의 `principal.getTenantId()`**에서만 얻습니다. path/header/body로 받지 않습니다.
- Controller는 read/update/delete에서 `principal.getTenantId()`를 Service로 전달만 하고, 검증 분기(if)는 두지 않습니다.
- Service는 `(id, tenantId)`를 Port에 전달합니다. update는 반드시 `findById(id, tenantId)`로 소유권을 검증한 뒤 `save()` 합니다. create는 부모 aggregate 존재 검증도 `findById(parentId, command.tenantId())`로 tenant 범위에서 수행합니다.
- Adapter/JpaRepository는 WHERE 절에 `tenant_id`를 포함(`findByXxxIdAndTenant_TenantId`, `findAllByTenant_TenantId`)해 다른 tenant 행을 애초에 읽지 못하게 합니다. 삭제는 `deleteByXxxIdAndTenantId`로 원자 삭제 후 `deletedCount == 0`이면 `NOT_FOUND`.
- 소유권 불일치는 **404 `NOT_FOUND`**로 처리합니다(403이 아님 — 리소스 존재 자체를 은닉).
- 중복체크(`existsBy...`) 중 부모ID를 포함하는 것(`existsByNameAndClientId` 등)은 부모ID가 이미 tenant에 종속되므로 tenant 파라미터를 두지 않습니다. 부모 없는 전역 유일 체크는 tenant를 포함합니다(`existsByNameKrAndTenantId`).

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
