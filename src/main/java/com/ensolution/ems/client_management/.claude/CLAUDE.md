# client_management 모듈 가이드라인

측정대행 의뢰기관, 사업장, 측정시설(굴뚝)과 그 하위 설비·측정물질을 관리하는 모듈입니다.

## 도메인 모델 구조

```
Client (의뢰기관)
  └── Workplace (사업장)  1:N
        └── Stack (측정시설/굴뚝)  1:N
              ├── Facility (배출시설)  1:N
              └── Prevention (방지시설)  1:N

PollutantCatalog (측정물질 가이드 · 전역)
  └── Pollutant (고객사 채택 물질)  1:N  ──< StackPollutant (시설별 측정물질) >── Stack

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
| `PollutantCatalog` | 고객사에게 **지원하는 측정물질 가이드**. **이 모듈 유일의 tenant 비종속 애그리거트**(전 tenant 공유). 불변 키 `code`를 보유해 클라이언트가 물질별 분기를 할 수 있게 한다. code는 **측정분야 안에서만 유일**하다(대기 납·수질 납이 모두 `PB`). 표기값(영문명·시험장비·시험방법)은 **소유하지 않는다** |
| `Pollutant` | 고객사가 가이드에서 **채택한** 물질. `catalogId`는 필수 — 가이드에 없는 물질은 만들 수 없다. `nameKr`·`nameEn`·`equipment`·`testMethod`는 고객사 소유 컬럼이고, `code`·`field`·`method`·`phase`는 카탈로그에서 조인해 채우는 읽기 전용 투영값이다 |
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
| `FacilityService` | `createFacility`, `getFacility`, `getFacilityList(stackId)`, `updateFacility`, `reorderFacilities`, `deleteFacility`. 등록 시 `max(sortOrder)+10`을 부여해 목록 맨 뒤에 붙인다 |
| `PreventionService` | `createPrevention`, `getPrevention`, `getPreventionList(stackId)`, `updatePrevention`, `reorderPreventions`, `deletePrevention`. 등록 시 `max(sortOrder)+10`을 부여해 목록 맨 뒤에 붙인다 |
| `PollutantCatalogService` | 운영자용 가이드 CRUD + `deactivateCatalog`/`activateCatalog`, 시드용 멱등 `ensureCatalog`. tenant 범위를 다루지 않는다 |
| `PollutantService` | `createPollutant`(가이드 항목 채택), `getPollutant`, `getPollutantList(field)`(채택분만), `getPollutantCandidates(field)`(미채택 가이드 항목), `updatePollutant`(고객사 소유값만), `deletePollutant`. 후보 목록만 `PollutantCatalogAssembler`에 위임 |
| `StackPollutantService` | `createStackPollutant`, `createStackPollutants`(일괄), `getStackPollutantList(stackId)`, `removeStackPollutant`. **등록 대상은 이미 채택한 `pollutantId`만** — 등록 과정에서 측정물질을 만들지 않는다 |
| `TeamService` | `createTeam`, `getTeam`, `getTeamList`, `updateTeam`, `deleteTeam`. 사수·부사수 user 검증은 `TeamValidator`에 위임(아래 참고). 장비 id는 검증 없이 저장 |

### 검증 (Validator)
비즈니스 규칙 검증은 `application/validator/`의 `{애그리거트}Validator`가 담당합니다.
서비스는 조건식 대신 `require*()` 호출만 남깁니다. (루트 `CLAUDE.md` 10번 규칙 — 이 모듈이 레퍼런스 구현입니다.)

| Validator | 메서드 | 규칙 |
|-----------|--------|------|
| `ClientValidator` | `requireUniqueName(name)` | 의뢰기관명 유일 |
| `WorkplaceValidator` | `requireUniqueNameInClient(name, clientId)` | 사업장명은 의뢰기관 내 유일 |
| `StackValidator` | `requireUniqueNameInWorkplace(name, workplaceId, field)` | 측정시설명은 사업장·측정분야 내 유일 |
| `PollutantValidator` | `requireSelectable(catalog)` | 폐지된(`active=false`) 가이드 항목은 새로 채택할 수 없음. 저장소 재조회를 피하려 도메인 객체를 받는다 |
| | `requireCatalogNotLinked(catalogId, tenantId)` | 한 tenant는 같은 가이드 항목을 두 번 채택할 수 없음 |
| `PollutantCatalogValidator` | `requireUniqueCode(field, code)` | 가이드 키는 측정분야 안에서 유일 |
| | `requireNotReferenced(catalogId)` | 참조 중인 카탈로그 삭제 차단(폐지는 `active=false`로) |
| `StackPollutantValidator` | `requirePollutantOwned(pollutantId, tenantId)` | 등록 대상이 이 tenant가 채택한 물질인지 확인. 미존재·타 tenant 모두 `NOT_FOUND`로 은닉 |
| | `requirePollutantsOwned(commands, tenantId)` | 일괄 등록용. 이 tenant의 측정물질을 **한 번만 읽어** 대조한다(항목별 조회 금지) |
| | `requireNotRegistered(stackId, pollutantId)` | 같은 시설에 같은 물질 중복 등록 불가 |
| | `requireNoDuplicatesInBatch(commands)` | 일괄 등록 요청 내부의 자기 중복 차단(부분 저장 방지) |
| `TeamValidator` | `requireUniqueName(name, tenantId)` | 팀명은 tenant 내 유일 |
| | `requireMemberInTenant(userId, tenantId, notFound)` | 사수·부사수 user가 존재하고 해당 tenant 소속인지 `auth`의 `UserQueryUseCase`로 확인. 미존재·타 tenant 모두 `TEAM_MENTOR_NOT_FOUND`/`TEAM_MENTEE_NOT_FOUND`로 은닉 |
| `FacilityValidator` | `requireExactOrder(current, orderedIds)` | 순서 변경 요청이 그 측정지점의 배출시설 집합과 정확히 일치해야 함(중복·누락·미지의 id 거부). 서비스가 이미 읽은 목록을 받아 대조하므로 포트를 주입받지 않는다 |
| `PreventionValidator` | `requireExactOrder(current, orderedIds)` | 방지시설에 대해 위와 동일 |

- **Validator가 다루지 않는 것**: 단건 존재·소유권 검증은 그대로 Adapter의 `findById(id, tenantId)` + `NOT_FOUND`가 담당합니다(아래 "tenant 소유권 격리" 참고). 형식·필수값은 Request DTO의 Bean Validation이 담당합니다.
  - 예) 측정물질 채택의 `catalogId` **필수**는 `CreatePollutantRequest`의 `@NotNull`이, **존재 여부**는
    `PollutantCatalogRepository.findById()`의 `POLLUTANT_CATALOG_NOT_FOUND`가 맡습니다. Validator는 폐지 여부만 봅니다.
  - 단 **다른 애그리거트를 참조하는 검증은 Validator가 맡습니다.** `StackPollutantValidator.requirePollutantOwned`가
    그 예로, 등록하려는 `pollutantId`가 이 tenant의 측정물질인지 확인합니다. 자기 애그리거트 단건을 읽는 것이 아니라
    참조 대상의 존재·소속을 보는 규칙이므로, 서비스 본문에 조건식을 남기지 않도록 Validator에 둡니다.
- Validator는 port/out과 타 모듈 port/in만 주입받고 `@Transactional`을 갖지 않습니다.
- 중복 체크 파라미터에 tenant를 넣는 기준은 아래 "tenant 소유권 격리"의 `existsBy...` 규칙과 동일합니다.

### 조립 (Assembler)

조립 협력자는 `application/service/assembler/`에 둡니다. 전부 `@Component`이며 `@Transactional`을 갖지 않습니다.
(`TeamAssembler`만 아직 `service/` 직하에 있으며 이관 예정입니다.)

- `StackDetailAssembler` — Stack과 하위 aggregate(Facility·Prevention)를 각 Outbound Port로 읽어
  `StackDetail`로 조립합니다. 트리 조립 책임을 서비스·도메인 모델에서 분리한 `@Component`입니다.
- `StackService.getStackDetail()`이 이 Assembler에 위임합니다.
- **카탈로그 속성 투영은 Assembler가 아니라 `PollutantEntityMapper`가 담당합니다.** `toDomain()`이 `code`·`field`·
  `method`·`phase`를 `catalog.*`에서 매핑하므로, 측정물질을 읽는 모든 경로가 한 곳에서 같은 값을 얻습니다.
  덕분에 도메인에 병합 로직이 없고 조회 경로에서 카탈로그를 다시 읽지 않습니다.
- `PollutantCatalogAssembler` — 가이드와 이 tenant의 **채택 현황**을 대조해 `assembleCandidates()`로 **아직 채택하지 않은 항목**을
  뽑습니다. 값을 병합하지는 않습니다(표기값은 `Pollutant`가 소유하고, 카탈로그 속성은 위 매퍼가 이미 채웠습니다).
  조회는 소스당 1회씩 총 2회로 고정하고 `Set`으로 in-memory 대조합니다(항목별 조회 금지).
  - 후보는 `findAllActive`로 읽습니다. 폐지 항목은 새로 채택할 수 없으므로 후보에서 뺍니다 — 이미 채택해 쓰고 있는
    폐지 물질은 채택분 목록(`getPollutantList`)이 계속 보여 줍니다.
  - `pollutantById(tenantId)`는 카탈로그를 읽지 않으므로 조회 1회입니다.
- `TeamAssembler` — Team 도메인에 타 모듈(`auth`)의 사수·부사수 **이름**을 결합해 `TeamDetail`/`TeamListItem`으로 조립합니다.
  users는 별도 모듈이라 JPA 조인 불가 → `UserQueryUseCase`(인바운드 포트)로 채웁니다. 목록은 `getUserList(tenantId)` 1회 + `Map`으로 N+1을 회피합니다.
- `StackSnapshotAssembler` — 측정시설 트리를 타 모듈 공개용 `StackMeasurementSummary`로 조립합니다.
  `schedule`이 측정 시점 스냅샷을 만들 때 이 경로로 원장을 읽습니다. 조회 결과를 그대로 복사해 가므로
  이후 원장이 바뀌어도 과거 회차의 성적서는 흔들리지 않습니다.

### 외부 공개 (Inbound Port)

`application/port/in/`에 UseCase와 공개 VO를 함께 둡니다.

| 공개 계약 | 구현체 | 소비 모듈 |
|---|---|---|
| `WorkplaceQueryUseCase` | `WorkplaceService` | `contract`(사업장 존재 확인·요약), `schedule` |
| `StackQueryUseCase` | `StackService` | `schedule`(측정 시점 시설·측정항목 스냅샷) |
| `TeamQueryUseCase` | `TeamService` | `schedule`(측정 팀 스냅샷) |

공개 VO — `ContractSummary` · `StackMeasurementSummary` · `StackMeasurementItemSummary` · `TeamSummary` · `UserTeamSummary`

> 공개 계약을 넓힐 때는 **호출자가 실제로 필요한 것만** 추가합니다(ISP). Repository 전체를 노출하지 않습니다.

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
- `application/port/out/FacilityRepository` — `save()`, `saveAll()`, `findById(id, tenantId)`, `findByStackId(stackId, tenantId)`, `findMaxSortOrder(stackId, tenantId)`, `deleteById(id, tenantId)`
- `application/port/out/PreventionRepository` — `save()`, `saveAll()`, `findById(id, tenantId)`, `findByStackId(stackId, tenantId)`, `findMaxSortOrder(stackId, tenantId)`, `deleteById(id, tenantId)`

> `findByStackId`는 `sort_order` 오름차순으로 정렬해 돌려줍니다(tie-breaker는 PK). **배열 순서가 곧 표시 순위**이며,
> 이 정렬은 `StackDetailAssembler`를 거쳐 측정계획 스냅샷·엑셀 성적서의 시설 나열 순서까지 그대로 전파됩니다.
- `application/port/out/PollutantCatalogRepository` — `save()`, `findById(id)`, `findByFieldAndCode(field, code)`, `findAll(field)`, `findAllActive(field)`, `existsByFieldAndCode(field, code)`. **전역 마스터이므로 tenantId 파라미터가 없습니다** — 이 모듈의 유일한 예외입니다.
  - code 조회에 `field`를 함께 받는 이유는 code의 유일 범위가 측정분야이기 때문입니다(위 "전역 마스터" 참고).
- `application/port/out/PollutantRepository` — `save()`, `findById(id, tenantId)`, `findByField(field, tenantId)`, `findAll(tenantId)`, `findByCatalogIdOrNull(catalogId, tenantId)`, `existsByCatalogId(catalogId)`, `deleteById(id, tenantId)`
  - `findByCatalogIdOrNull`은 미존재 시 예외 대신 **null을 반환**합니다. "아직 채택하지 않았다"는 정상 상태이며 호출부가 행 생성 여부를 판단하기 때문입니다.
  - `findByField`는 `pollutants`에 `field` 컬럼이 없으므로 **카탈로그의 `field`로 거릅니다.**
- `application/port/out/StackPollutantRepository` — `save()`, `findByStackId(stackId, tenantId)`, `existsByStackIdAndPollutantId()`, `deleteById(id, tenantId)`
- `application/port/out/TeamRepository` — `save()`, `findById(id, tenantId)`, `findAll(tenantId)`, `existsByNameAndTenantId()`, `deleteById(id, tenantId)`
- 부모-자식 조회는 반드시 Port 메서드로 추상화합니다. (예: `findByClientId(Long clientId, Long tenantId)`)
- `findById()` 는 `Optional` 을 반환하지 않고 `T` 를 직접 반환합니다. 없으면 Adapter에서 `NOT_FOUND` 예외를 던집니다.

### 전역 마스터 (명시적 예외)

`PollutantCatalog`는 이 모듈에서 **유일하게 tenant에 종속되지 않는 애그리거트**입니다. 지원 물질 목록은 모든
고객사에 동일하게 적용되며, 클라이언트가 특정 물질에 반응형 동작을 하려면 전 tenant 공통 키가 필요하기 때문입니다.

**가이드는 "무엇을 쓸 수 있는가"만 정의합니다.** tenant별 표기명·시험장비·공정시험법은 `Pollutant`가 직접 소유하며,
가이드를 고쳐도 이미 채택한 고객사의 표기는 바뀌지 않습니다. 반대로 `field`·`method`·`phase`는 가이드가 단일 진실
소스라 조인으로 전파되므로 법령 개정이 즉시 반영됩니다.

**고객사는 가이드에 없는 물질을 만들 수 없습니다** (`pollutants.catalog_id` NOT NULL). 따라서 가이드에 항목이 없는
측정분야는 물질 등록 자체가 불가능합니다 — 현재 시드는 `AIR`뿐이므로 다른 분야를 열려면 카탈로그 확충이 선행되어야 합니다.

- `pollutant_catalog`에는 `tenant_id`도 `TenantEntity` 연관도 없습니다. 조회에 tenant 조건을 걸지 않는 것이 정상입니다.
- 관리 API는 **클래스는 이 모듈에 두고 URL만 `/api/platform/pollutant-catalog`**로 잡습니다
  (`PlatformPollutantCatalogController`). `SecurityConfig`가 `/api/platform/**`를 `PLATFORM_ADMIN`으로 이미 보호하므로
  SecurityConfig 수정이 필요 없고, `client_management → platform` 참조 금지 규칙도 지켜집니다.
- **카탈로그는 하드 삭제하지 않습니다.** 폐지는 `active=false`이며, 이미 채택해 쓰고 있는 tenant에게는 계속 노출합니다.
  단 **새로 채택하는 것은 막습니다**(`POLLUTANT_CATALOG_INACTIVE`).
- `code`는 부여 후 변경하지 않습니다. 클라이언트 분기와 측정계획 스냅샷이 이 값에 의존합니다.
- **code의 유일 범위는 측정분야입니다** (`uk_pollutant_catalog_field_code`). 같은 물질이라도 대기와 수질은
  배출허용기준·공정시험법이 다른 별개 항목이고, 중금속은 양쪽 법령에 모두 있어 전역 유일로 두면 한쪽만 등록됩니다.
  - 그래서 **code만으로 물질을 특정할 수 없습니다.** 측정물질 생성·시설별 측정물질 등록 등 카탈로그를 지목하는
    모든 경로는 `catalogId`를 받습니다. code는 클라이언트가 화면(측정분야) 안에서 분기하는 용도입니다.

### 부트스트랩 (계층 규칙의 명시적 예외)

`infrastructure/bootstrap/PollutantCatalogInitializer`는 infrastructure에 있으면서 `@Transactional` + 아웃바운드 포트 +
유스케이스를 직접 조합합니다. 런타임 유스케이스가 아니라 **배포 1회성 설치 코드**이므로 현 위치를 유지하며,
**예외 범위는 이 클래스에 한정**합니다(platform 모듈의 `PlatformAdminInitializer`와 같은 성격).
Spring Data Repository를 직접 주입하지 않고 `application/port/out`만 씁니다.

### tenant 소유권 격리 (멀티테넌시)

**전역 규칙입니다 — 루트 `CLAUDE.md` 규칙 13을 따릅니다.** 이 모듈이 그 레퍼런스 구현이며,
규칙 본문의 예시(`findByXxxIdAndTenant_TenantId`, `deleteByXxxIdAndTenantId`)는 여기서 나왔습니다.

이 모듈에만 해당하는 것:

- `PollutantCatalog`는 **이 모듈 유일의 tenant 비종속 애그리거트**입니다(위 "전역 마스터" 참고).
  `pollutant_catalog`에는 `tenant_id`도 `TenantEntity` 연관도 없으므로 조회에 tenant 조건을 걸지 않는 것이 정상입니다.
- 중복체크 파라미터 예시 — 부모ID를 포함하는 `existsByNameAndClientId`는 부모가 이미 tenant에 종속되므로
  tenant를 받지 않고, 부모 없는 전역 유일 체크(`existsByNameAndTenantId`)는 받습니다.

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
