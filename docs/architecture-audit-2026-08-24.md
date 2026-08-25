# 아키텍처 규칙 준수 진단 리포트

**점검일** 2026-08-24
**범위** `src/main/java/com/ensolution/ems/**` 전체 (커밋되지 않은 워킹트리 변경 199개 포함), `src/test/**`, 루트 `CLAUDE.md`·`ARCHITECTURE.md`, 모듈별 `.claude/CLAUDE.md` 4개, `docs/DATABASE.md`
**기준 문서** 루트 `CLAUDE.md` 전역 규칙 12개
**성격** 진단 전용 — 이 리포트 외에 코드·문서를 수정하지 않았습니다.

각 항목은 `[코드]` / `[문서]` / `[판단]` 으로 태깅했습니다.
`[코드]`는 코드를 규칙에 맞추면 해소, `[문서]`는 코드가 맞고 문서가 틀린 것, `[판단]`은 어느 쪽이 옳은지 결정이 필요한 것입니다.

---

## 0. 요약

| 심각도 | 건수 | 성격 |
|---|---|---|
| ~~치명~~ | ~~3~~ | ✅ **2026-08-25 조치 완료** — 인증 없는 권한 상승 · admin/contract 모듈 테넌트 격리 부재 |
| 높음 | 4 | 레이어·모듈 경계 실질 위반 |
| 중간 | 13 | 규칙 명문 위반 (포트 위치·시그니처·응답·네이밍) |
| 경미 | 12 | 컨벤션 이탈 |
| 문서 드리프트 | 12 | 코드가 맞고 문서가 틀림 |
| 문서 공백 | 7 | 규칙이 없거나 모듈 문서 부재 |
| 준수 확인 | 11 | 전수 검색으로 위반 0건 확인 |

**한 줄 결론**: 레이어 의존성(규칙 1)과 생성자 주입(규칙 5)은 전 코드베이스에서 거의 완벽하게 지켜지고 있습니다. 그러나 **아키텍처와 별개로 즉시 조치가 필요한 보안 결함 3건이 점검 중 드러났습니다**(§1). 아키텍처 측면의 문제는 ① `tenant` 모듈이 사라진 사실이 문서에 반영되지 않아 루트·모듈 문서 7곳이 존재하지 않는 경로를 가리킨다는 점, ② 최대 규모 모듈 중 하나인 `schedule`(163파일)에 모듈 문서가 없어 비표준 구조 다수가 무근거로 남아 있다는 점입니다.

> **먼저 읽어야 할 곳**: §1은 규칙 준수 문제가 아니라 실제 취약점입니다. 아키텍처 정리보다 우선합니다.

---

## 1. 치명 — 테넌트 격리·권한 검증 부재 `[코드]` — ✅ 2026-08-25 조치 완료

> **조치 상태**: 아래 1-A·1-B·1-C 세 건은 **2026-08-25에 모두 수정했습니다.** 각 절 끝의 "적용 내역"을 참고하세요.
> 전체 테스트 361개 통과 및 Spring 컨텍스트 기동을 확인했습니다. 아래 본문은 수정 전 상태의 기록입니다.
>
> 조치 과정에서 원 리포트가 놓친 두 경로를 추가로 발견해 함께 막았습니다.
> - `GET /api/contracts?workplaceId=` — 타 테넌트 사업장 id를 넘기면 그 계약 목록이 노출됐습니다(`findByWorkplaceId`에 tenant 조건 없음). 원 리포트의 "목록은 안전" 서술은 틀렸습니다.
> - `POST /api/contracts` — `WorkplaceQueryUseCase.existsById(workplaceId)`에 tenant 조건이 없어 타 테넌트 사업장을 참조할 수 있었습니다.
>
> 또한 **역할 부여 제한이 `createUser`에도 필요**했습니다. 수정 전에는 테넌트 ADMIN이 `PUT`(자기 승격)뿐 아니라
> `POST /api/admin/members`로 **PLATFORM_ADMIN 계정을 신규 생성**할 수도 있었습니다.


`client_management/.claude/CLAUDE.md`의 "tenant 소유권 격리" 규칙(모든 애그리거트는 로그인 사용자의 tenant 범위 안에서만 조회/수정/삭제)은 `client_management`·`equipment`·`schedule`·`storage`에는 일관되게 적용돼 있으나, **`auth`·`admin`·`contract` 세 모듈에는 적용되지 않았습니다.** 이 중 `auth`의 회원가입 경로는 인증조차 요구하지 않습니다.

세 건은 성격이 달라 따로 다룹니다. 심각도 순입니다.

---

### 1-A. 인증 없는 권한 상승 — `POST /api/auth/sign-up`

**가장 심각합니다.** 인증 없이 임의 테넌트에 임의 역할의 계정을 만들 수 있습니다.

`global/security/config/SecurityConfig.java:74-83`

```
:75  "/api/auth/sign-up",     ← permitAll 목록에 포함
:76  "/api/auth/sign-in",
:77  "/api/auth/refresh", ...
:83  ).permitAll()
```

`auth/presentation/request/SignUpRequest.java`

```
:7-10  @NotNull Long tenantId,   ← 클라이언트가 지정
       @NotNull Long roleId,     ← 클라이언트가 지정
```

`auth/application/service/AuthService.java:75` `signUp(...)` → `:88` `register(...)`

```
:98    if (userRepository.existsByUsername(username)) { ... }   ← 아이디 중복만 확인
:103   roleRepository.findById(roleId);                          ← "존재하는 역할인가"만 확인
```

`auth/domain/User.java:23-43` `signUp(...)` — 전달받은 `tenantId`·`roleId`를 그대로 빌더에 넣을 뿐 **어떤 불변식도 검사하지 않습니다.**

즉 검증되는 것은 "아이디가 중복이 아닌가"와 "그런 역할이 존재하는가" 둘뿐이며, **호출자가 그 테넌트에 가입할 자격이 있는지, 그 역할을 부여받을 자격이 있는지는 아무도 확인하지 않습니다.**

**공격 시나리오**

1. 익명 요청자가 `roleId=1`, 임의 `tenantId`로 sign-up → 계정 획득
2. 로그인 후 `GET /api/roles`(인증만 필요, `auth/presentation/controller/RoleController.java:26`)로 **역할 id 전체를 열람** → `PLATFORM_ADMIN`의 id 확인
3. 그 id로 다시 sign-up → **플랫폼 운영자 계정 획득**. 이후 `/api/platform/**` 전체(테넌트 발급·조회)에 접근

표준 역할은 `PlatformAdminInitializer`가 부팅 시 생성합니다(`platform/infrastructure/bootstrap/PlatformAdminInitializer.java:32-38, 61`). 생성 순서는 `Map.of(...)`의 `forEach`라 **JVM 실행마다 달라져 id를 예측할 수는 없지만**, 역할 수가 4개뿐이고 2번 단계에서 목록을 그대로 읽을 수 있으므로 실질적인 방어가 되지 않습니다.

**수정 방향** (택1 또는 병행)

- sign-up을 공개 엔드포인트에서 제거 — 회원 생성은 이미 `POST /api/admin/members`(ADMIN 전용)가 담당하므로 기능 중복이기도 합니다
- 공개 가입을 유지해야 한다면 `tenantId`·`roleId`를 **요청에서 받지 않고** 서버가 결정(가입 코드/초대 토큰 기반)하고, 부여 가능한 역할을 `USER`로 고정
- 어느 쪽이든 `roleRepository.findById(roleId)`(존재 확인)를 **권한 검증으로 착각하지 않도록** 도메인에 `User.requireAssignableRole(...)` 같은 불변식을 두는 편이 안전합니다

#### 적용 내역 (2026-08-25)

**엔드포인트를 제거**했습니다. 프론트엔드에 호출부가 없고(`ems-web`의 `SignInForm.tsx:39`에 라우트 없는 링크만 존재), 회원 생성은 `POST /api/admin/members`가, 테넌트 최초 관리자는 `platform`의 `provisionTenant`가 이미 담당하므로 기능 손실이 없습니다.

- 삭제: `auth/presentation/request/SignUpRequest.java`, `auth/presentation/mapper/SignUpRequestMapper.java`, `auth/application/command/SignUpCommand.java`
- `AuthController` — `signup` 엔드포인트와 `signUpMapper` 의존 제거. 공개 가입을 두지 않는 이유를 주석으로 남김
- `AuthService` — `signUp(...)` 제거. `register(...)`는 `createUser` 전용으로 남음
- `SecurityConfig` — permitAll 목록에서 `/api/auth/sign-up` 제거

### 1-B. 권한 상승 + 교차 테넌트 — `PUT /api/admin/members/{id}`

`/api/admin/**`는 `hasRole("ADMIN")`으로 보호되지만(`SecurityConfig.java:73`), **어느 테넌트의 ADMIN인지는 확인하지 않습니다.**

`admin/presentation/controller/MemberController.java`

```
:54  public ResponseEntity<...> getMember(@PathVariable Long id)                      ← principal 없음
:64  public ResponseEntity<...> updateMember(@PathVariable Long id, @RequestBody ...)  ← principal 없음
:74  public ResponseEntity<...> deleteMember(@PathVariable Long id)                    ← principal 없음
```

같은 파일의 `createMember:34-38`·`getMemberList:44-48`은 `@AuthenticationPrincipal`을 받아 `principal.getTenantId()`를 넘깁니다. 단건 경로 3개만 빠졌습니다.

`admin/application/service/MemberService.java:31, 35, 40` → `auth/application/port/in/UserQueryUseCase.getUser(Long userId)`, `UserCommandUseCase.updateUser/deleteUser(Long userId)` — **포트 시그니처 자체에 `tenantId`가 없습니다.** 같은 파일 `:45` `getUserList(tenantId)`는 tenant를 받으므로, 목록 조회만 격리되고 단건 경로는 뚫려 있는 구조입니다.

`auth/application/service/UserService.java:26-30`, `AuthService.java:49-67, 69-73` — `findById(userId)` 후 tenant 대조 없이 바로 반환·수정·삭제.

**영향 두 가지**

1. **교차 테넌트**: A사 ADMIN이 B사 사용자 계정을 조회·수정·삭제할 수 있습니다.
2. **자기 권한 상승**: `UpdateUserCommand`에 `roleId`가 포함되고(`auth/application/port/in/UpdateUserCommand.java:5`) `AuthService.updateUser:54-56`은 역할의 **존재 여부만** 확인합니다. 따라서 테넌트 ADMIN이 자기 자신의 `id`로 `roleId`를 `PLATFORM_ADMIN`으로 바꿔 **플랫폼 운영자로 승격**할 수 있습니다. 역할 목록은 `GET /api/roles`(`auth/presentation/controller/RoleController.java:26`, 인증만 필요)로 그대로 열람 가능합니다.

**수정 방향**

1. 컨트롤러 3개에 `@AuthenticationPrincipal` 추가
2. `UserQueryUseCase.getUser(Long userId, Long tenantId)`, `UserCommandUseCase.updateUser/deleteUser`에 tenantId 추가 → `UserRepository`·어댑터까지 전파
3. **역할 부여 규칙을 별도로 세울 것** — tenant ADMIN이 부여할 수 있는 역할을 `ADMIN`/`USER`로 제한. 테넌트 격리를 고쳐도 자기 계정 승격 경로는 남으므로 tenantId 추가만으로는 해결되지 않습니다
4. `GET /api/roles`가 `PLATFORM_ADMIN`을 노출할 필요가 있는지 재검토

#### 적용 내역 (2026-08-25)

**테넌트 격리**

- `MemberController` — `getMember`·`updateMember`·`deleteMember`에 `@AuthenticationPrincipal` 추가, `principal.getTenantId()` 전달
- `MemberService` — `getMember(id, tenantId)`, `deleteMember(id, tenantId)`. `UpdateMemberCommand`·`UpdateUserCommand`에 `tenantId` 필드 추가(`MemberMapper`·`MemberPortMapper` 경유 전달)
- `UserQueryUseCase.getUser(userId, tenantId)`, `UserCommandUseCase.deleteUser(userId, tenantId)` — 포트 시그니처에 tenant 반영
- `UserRepository.findById(id, tenantId)`, `deleteById(id, tenantId)` → `UserJpaRepository.findByUserIdAndTenantId` / `deleteByUserIdAndTenantId`. 삭제는 0건이면 `USER_NOT_FOUND`
- 소유권 불일치는 404 `USER_NOT_FOUND`로 은닉(403 아님)

**역할 부여 제한**

- `Role.PLATFORM_ADMIN` 상수 + `isPlatformAdmin()` 도메인 메서드 추가
- 신규 `auth/application/validator/UserValidator.requireAssignableRole(roleId)` — 존재하지 않으면 `ROLE_NOT_FOUND`, `PLATFORM_ADMIN`이면 신규 `ROLE_NOT_ASSIGNABLE`(403)
- `AuthService`의 **`createUser`와 `updateUser` 양쪽**에 적용. 생성 경로에도 필요했습니다 — 없으면 테넌트 ADMIN이 PLATFORM_ADMIN 계정을 새로 만들 수 있습니다
- `UserCommandUseCase.createPlatformAdmin(...)` 신설 — 부트스트랩(`PlatformAdminInitializer`)이 운영자 계정을 만드는 **유일한 예외 경로**. 이걸 두지 않으면 서버 최초 기동이 `ROLE_NOT_ASSIGNABLE`로 실패합니다
- `platform`의 `provisionTenant`는 `ADMIN` 역할을 이름으로 조회하므로 영향 없음

**여파 반영** — `getUser` 시그니처 변경으로 `client_management`의 `TeamValidator.requireMemberInTenant`(수동 tenant 대조 제거, 포트가 담당)와 `TeamAssembler.resolveName(userId, tenantId)`를 함께 수정했습니다. 테스트 fake 2개(`TeamValidatorTest`, `TeamServiceTest`)도 갱신했습니다.

### 1-C. 교차 테넌트 — `contract` 단건 경로

`client_management/.claude/CLAUDE.md`의 tenant 격리 규칙이 `contract` 모듈에는 전혀 적용되지 않았습니다.

> **작성 중 상태 변화**: 이 점검이 진행되는 동안 `ContractController`에 `@AuthenticationPrincipal`이 추가되었습니다. 아래는 **그 변경을 반영한 현재 상태**이며, 결론은 달라지지 않았습니다 — 파라미터만 추가되고 실제로 쓰이지 않아 **취약점은 그대로입니다.**

#### 근거

`contract/presentation/ContractController.java` — principal을 받기는 하나 **사용하지 않습니다.**

```
:56-61  getContract(@PathVariable Long contractId, @AuthenticationPrincipal CustomUserDetails principal)
:60         contractService.getContract(contractId);              ← principal 미사용
:66-73  updateContract(..., @AuthenticationPrincipal ... principal)
:71         contractService.updateContract(contractId, ...);      ← principal 미사용
:77-83  deleteContract(..., @AuthenticationPrincipal ... principal)
:81         contractService.deleteContract(contractId);           ← principal 미사용
```

`contract/application/service/ContractService.java` — tenantId를 받는 시그니처가 없습니다.

```
:49-50  updateContract(Long contractId, UpdateContractCommand command) → contractRepository.findById(contractId)
:69-70  deleteContract(Long contractId)                              → contractRepository.deleteById(contractId)
:79-80  getContract(Long contractId)                                 → contractRepository.findById(contractId)
:84     getContractList(Long workplaceId, Long tenantId)             ← 목록만 tenant를 받음
```

`contract/domain/port/ContractRepository.java`

```
:10  Contract findById(Long id);
:13  void deleteById(Long id);
```

`contract/infrastructure/adapter/ContractRepositoryAdapter.java`

```
:34-38  jpaContractRepository.findById(id)      // WHERE 절에 tenant_id 없음
:51-53  jpaContractRepository.deleteById(id)    // 동일
```

#### 영향

인증된 사용자라면 누구나 `GET /api/contracts/{id}`, `PUT /api/contracts/{id}`, `DELETE /api/contracts/{id}`로 **다른 고객사의 계약을 조회·수정·삭제**할 수 있습니다. 계약에는 계약금액·기간·사업장 정보가 포함되므로 노출 영향이 큽니다.

비교: 다른 13개 포트는 모두 `findById(Long id, Long tenantId)` 시그니처를 일관되게 씁니다(`client_management` 8개, `equipment`·`schedule`·`storage` 각 1~2개). `contract`만 예외입니다.

#### 수정 방향

1. 컨트롤러 3개가 이미 받고 있는 `principal.getTenantId()`를 **서비스로 실제 전달** (현재는 파라미터만 선언된 상태)
2. `ContractService`의 `getContract`/`updateContract`/`deleteContract`에 `tenantId` 파라미터 추가, `ContractRepository.findById(Long id, Long tenantId)` / `deleteById(Long id, Long tenantId)`로 시그니처 변경
3. `ContractJpaRepository`에 `findByIdAndTenantId` / `deleteByIdAndTenantId` 추가, 어댑터에서 `deletedCount == 0`이면 `NOT_FOUND`
4. 소유권 불일치는 403이 아니라 **404 `NOT_FOUND`** — 리소스 존재 자체를 은닉(client_management 규칙과 동일)

> ~~참고: `findAllByTenantId(tenantId)`(목록)와 `findByWorkplaceId(workplaceId)`는 이미 tenant 또는 tenant 종속 부모로 걸러지므로 안전합니다. 문제는 단건 경로뿐입니다.~~
> **정정(2026-08-25)**: `findByWorkplaceId(workplaceId)`는 안전하지 않았습니다. `workplaceId`가 호출자 tenant 소속인지 확인하지 않으므로 `GET /api/contracts?workplaceId=<타 테넌트 사업장 id>`로 목록이 노출됐습니다. 조치에 포함했습니다.

#### 적용 내역 (2026-08-25)

- `ContractController` — 이미 선언돼 있던 `principal`을 실제로 서비스에 전달(`getContract`·`updateContract`·`deleteContract`)
- `ContractService` — 위 3개 메서드에 `tenantId` 파라미터 추가
- `ContractRepository.findById(id, tenantId)`, `deleteById(id, tenantId)` → `ContractJpaRepository.findByContractIdAndTenantId` / `deleteByContractIdAndTenantId`. 삭제 0건이면 `NOT_FOUND`
- **목록 경로**: `findByWorkplaceId(workplaceId, tenantId)` → `ContractTableViewJpaRepository.findByWorkplaceIdAndTenantId`
- **생성 경로**: `WorkplaceQueryUseCase.existsById(workplaceId, tenantId)`로 좁힘 → `WorkplaceRepository`·`WorkplaceService`·`WorkplaceRepositoryAdapter`(신규 `existsByWorkplaceIdAndTenant_TenantId`)까지 전파. 사용처는 `contract` 하나뿐이라 여파 없음
- `deleteByWorkplaceId`는 사업장 삭제 이벤트의 내부 캐스케이드(`WorkplaceDeletedEventListener`)이고 사업장 삭제 자체가 이미 tenant 범위이므로 그대로 둠

---

## 2. 높음 — 레이어·모듈 경계 실질 위반

### H1. `global`이 두 모듈의 JPA 계층을 직접 관통 `[코드]`

`global/security/user/CustomUserDetailsService.java`

```
:3-5   import com.ensolution.ems.auth.infrastructure.entity.RoleEntity;
       import com.ensolution.ems.auth.infrastructure.entity.UserEntity;
       import com.ensolution.ems.auth.infrastructure.repository.UserJpaRepository;
:8-9   import com.ensolution.ems.platform.infrastructure.entity.TenantEntity;
       import com.ensolution.ems.platform.infrastructure.repository.TenantJpaRepository;
:26-27 private final UserJpaRepository userRepository;
       private final TenantJpaRepository tenantJpaRepository;
```

규칙 4("Application Service는 Spring Data Repository를 직접 사용하지 않습니다")와 모듈 경계 규칙(ARCHITECTURE.md 189행 "다른 모듈의 기능이 필요할 때 반드시 해당 모듈의 Inbound Port를 통해 접근")을 동시에 위반합니다.

**프로젝트 전체에서 유일하게 뚫린 경로입니다.** application·presentation 계층의 infrastructure 참조는 0건인데 `global`만 예외적으로 두 모듈의 내부를 직접 읽습니다. `global`이 규칙 텍스트상 "기능 모듈"이 아니라는 이유로 지금까지 검토를 비껴간 것으로 보입니다.

수정 방향: `auth`의 `application/port/in/UserQueryUseCase`, `platform`의 `TenantQueryUseCase` 경유. 다만 `UserDetailsService`는 Spring Security 초기화 시점에 관여하므로 순환 참조 여부 확인이 필요합니다.

### H2. `schedule` 도메인이 `equipment` 도메인에 직접 결합 `[판단]`

`schedule/domain/snapshot/EquipmentSnapshot.java`

```
:3-5   import com.ensolution.ems.equipment.domain.InspectionItem;
       import com.ensolution.ems.equipment.domain.EquipType;
       import com.ensolution.ems.equipment.domain.spec.EquipmentSpec;
:16    EquipType type,
:23-24 List<InspectionItem> inspections,
       EquipmentSpec spec
```

**도메인 계층이 타 모듈 도메인에 결합**한 유일한 사례이며 모듈 경계상 가장 위험한 형태입니다.

다만 파일 javadoc(9-13행)에 "장비 유형별 사양은 equipment 도메인의 EquipmentSpec(sealed)을 그대로 보관하며, Spring Data MongoDB가 `_class` 판별자로 구현체를 복원한다"라고 **의도가 명시**돼 있고, `equipment/.claude/CLAUDE.md:91`도 "도메인 타입(`EquipType`, `InspectionType`, `InspectionItem`, `EquipmentSpec`)을 port/in에 그대로 노출합니다"라고 선언합니다.

즉 **의도된 설계이나 루트 규칙에 근거 조항이 없습니다.** 같은 성격의 import가 소비 모듈 12곳에 퍼져 있습니다.

| 파일 | 참조 타입 |
|---|---|
| `schedule/domain/snapshot/EquipmentSnapshot.java:3-5` | `InspectionItem`, `EquipType`, `EquipmentSpec` |
| `schedule/application/mapper/ScheduleExportViewMapper.java:3-7` | `EquipType`, `InspectionItem`, `InspectionType`, `PitotTubeType`, `spec.*` |
| `schedule/application/calculation/SheetCalculator.java:3-4` | `NozzleSpec`, `PitotTubeSpec` |
| `schedule/application/calculation/SheetContext.java:3-4` | `NozzleSpec`, `PitotTubeSpec` |
| `schedule/application/calculation/step/FlowStep.java:3` | `PitotTubeSpec` |
| `schedule/application/service/SnapshotSheetRecalculator.java:3-5` | `NozzleSpec`, `ParticleSamplerSpec`, `PitotTubeSpec` |
| `dashboard/application/command/InspectionDue.java:3` | `InspectionType` |
| `dashboard/presentation/response/InspectionDueResponse.java:3` | `InspectionType` |

`schedule`의 계산 로직이 `NozzleSpec`·`PitotTubeSpec` 같은 **하위 구체 타입**까지 파고든 것은 `equipment` 문서가 선언한 범위(`EquipmentSpec`)를 넘어섭니다.

결정이 필요한 두 갈래:

- **(A) 규칙에 예외를 명문화** — 루트 규칙 1에 "`port/in`이 공개 계약으로 노출하는 도메인 타입은 소비 모듈이 직접 참조할 수 있다(공유 커널)" 추가. 코드 변경 없음
- **(B) 타입을 승격** — 해당 enum·spec을 `equipment/application/port/in/` 또는 별도 공유 커널 패키지로 이동. 구조는 깨끗해지나 이동 비용과 Mongo `_class` 판별자 값 변경(기존 문서 마이그레이션) 부담

### H3. `contract` 도메인 포트의 역방향 의존 `[코드]`

`contract/domain/port/ContractRepository.java`

```
:3   import com.ensolution.ems.contract.application.command.ContractListItem;
:11  List<ContractListItem> findByWorkplaceId(Long workplaceId);
:12  List<ContractListItem> findAllByTenantId(Long tenantId);
```

`presentation → application → domain` 방향의 명백한 역주행입니다. 이 포트를 `application/port/out/`으로 이관하면(M3과 동일 작업) 자동 해소됩니다.

### H4. `platform/application/result/` — 규칙 8이 명문으로 금지한 패키지 `[코드]`

루트 `CLAUDE.md` 규칙 8: "`application/result/` 등 별도 패키지를 생성하지 않습니다."

전체 프로젝트에서 이 패키지는 `platform` 단 하나이며 2개 파일이 있습니다.

| 파일 | 올바른 위치 | 근거 |
|---|---|---|
| `platform/application/result/TenantSummary.java` | `application/port/in/` | `~Summary` = 타 모듈 공개용. javadoc에도 "타 모듈(schedule 등)이 사용" 명시 |
| `platform/application/result/TenantListItem.java` | `application/command/` | `~ListItem` = 목록 조회 VO |

두 파일의 목적지가 서로 다릅니다. 영향받는 import 10곳:

```
platform/application/mapper/TenantSummaryMapper.java:3
platform/application/port/in/TenantQueryUseCase.java:3
platform/application/port/out/TenantRepository.java:3       ← port/out이 result를 참조
platform/application/service/PlatformService.java:10, 13
platform/infrastructure/adapter/TenantRepositoryAdapter.java:5
platform/infrastructure/mapper/TenantEntityMapper.java:3
platform/presentation/mapper/TenantMapper.java:5
schedule/application/mapper/ScheduleSnapshotMapper.java:4         ← 크로스모듈
schedule/application/service/ScheduleSnapshotAssembler.java:6     ← 크로스모듈
```

크로스모듈 2곳은 **모듈 경계 위반이기도 합니다** — `schedule`이 `platform`의 `port/in`이 아니라 `application/result`를 직접 참조합니다. `TenantSummary`를 `port/in/`으로 옮기면 규칙 8 위반, 모듈 경계 위반, `platform/.claude/CLAUDE.md:31`의 문서 불일치가 **한 번에 셋 다** 해소됩니다.

---

## 3. 중간 — 규칙 명문 위반

### 포트·패키지 위치

| # | 항목 | 태그 |
|---|---|---|
| M1 | `auth/application/port/RefreshTokenStore.java` — 아웃바운드 포트(구현체 `infrastructure/adapter/RedisRefreshTokenStore`)인데 `port/` 직하. auth에는 `port/out/` 디렉터리 자체가 없음 | `[코드]` |
| M2 | `contract/application/port/ContractQueryUseCase.java` — 인바운드 포트인데 `port/` 직하. 같은 모듈 `application/port/in/ContractStatisticsUseCase`는 정상 위치라 **한 모듈 안에서 두 UseCase 위치가 갈림** | `[코드]` |
| M3 | `domain/port/` 잔존 — auth 6개(`Authenticator`·`PasswordEncryptor`·`RoleRepository`·`TokenIssuer`·`TokenParser`·`UserRepository`), contract 1개(`ContractRepository`). 루트 `CLAUDE.md` 규칙 4에 "향후 정렬 예정"으로 예고됐으나 미완 | `[코드]` |
| M4 | `schedule` presentation 그룹핑 불일치 — `analysis/`·`history/`는 애그리거트별 그룹핑인데 schedule 본체(`controller/`·`mapper/`·`request/`·`response/`)는 `presentation/` 직하 플랫. **한 모듈에 두 구조 공존** | `[코드]` |
| M5 | `contract/presentation/ContractController.java` — 유일하게 `presentation/` 직하. ARCHITECTURE.md 119-121행이 단일 애그리거트 모듈에 허용하나, 같은 조건인 auth를 포함해 나머지 전부 `controller/`를 씀 | `[판단]` |

> M1·M2는 파일 2개 이동 + import 3곳으로 끝나는 저비용 작업이며, 규칙 4의 "포트 위치 표준화"를 문자 그대로 만족시킵니다.

### 포트 시그니처 — 규칙 7 `Optional` 반환 금지

규칙 7: "Domain Port의 `findById` / `findBy{Field}` 는 `Optional` 을 반환하지 않습니다. Adapter 구현체에서 `.orElseThrow(...)` 로 처리하고, Port 인터페이스는 `T` 를 직접 반환합니다."

| # | 위치 | 시그니처 | 태그 |
|---|---|---|---|
| M6 | `platform/application/port/out/TenantRepository.java:20` | `Optional<Tenant> findByBizNumber(String)` | `[코드]` |
| M7 | `auth/domain/port/UserRepository.java:13, 17` | `Optional<User> findById(Long)`, `Optional<User> findByUsername(String)` | `[코드]` |
| M8 | `auth/domain/port/Authenticator.java:17` | `Optional<AuthenticatedUser> loadAuthenticatedUser(String)` | `[판단]` |

M6이 가장 명백합니다. **표준 위치(`application/port/out/`)의 포트**인 데다 같은 파일 13행 `Tenant findById(Long id)`는 규칙을 지키고 있어 **파일 내부에서 스스로 모순**입니다. 다만 이 메서드의 용도가 부트스트랩 멱등성 확인(존재하면 재사용, 없으면 생성)이라 예외를 주려면 규칙 쪽에 명시하는 편이 낫습니다.

M7의 `auth`는 규칙 4에서 `domain/port/` 위치에 대해서만 유예를 받았을 뿐, **`Optional` 금지 규칙에는 유예 문구가 없습니다.**

> `auth/domain/port/TokenParser.java:12`의 `Optional<String> extractUsername`은 javadoc에 "서명이 맞지 않거나 만료된 토큰은 예외 대신 `Optional.empty()`로 돌려줍니다"라고 의도를 명시했고 Repository 포트가 아니므로 **위반으로 보지 않았습니다.**

### domain → Spring 의존 (규칙 1)

| # | 위치 | 내용 | 태그 |
|---|---|---|---|
| M9 | `auth/domain/port/UserRepository.java:4, 9` | `import org.springframework.stereotype.Repository;` + 인터페이스에 `@Repository` 실제 부착. 아웃바운드 포트는 순수 인터페이스여야 하고 `@Repository`는 어댑터 쪽 관심사 | `[코드]` |
| M10 | `global/security/domain/JwtProperties.java:3`, `AuthCookieProperties.java:3` | `@ConfigurationProperties`. 성격상 도메인 모델이 아니라 인프라 설정인데 패키지명이 `domain`이라 규칙 텍스트에 걸림. `global/security/config/`로 옮기면 자연 해소 | `[코드]` |

### 응답·네이밍

| # | 위치 | 내용 | 태그 |
|---|---|---|---|
| M11 | `auth/presentation/controller/AuthController.java:45` | `public void signup(...)` — **반환 타입이 `void`**. 규칙 6("모든 엔드포인트는 `ApiResponse<T>`를 반환") 위반이며 프로젝트 유일의 미래핑 JSON 엔드포인트. `ResponseEntity<ApiResponse<Void>>`로 통일 필요 | `[코드]` |
| M12 | `dashboard/presentation/response/MeasurementCountChartResponse.java` | `Chart`는 UI 렌더링 방식. 규칙 7의 "`Table`, `Grid`, `Card` 등" 금지 취지에 정면 위반. 소스 VO가 `MeasurementCountItem`이므로 `MonthlyMeasurementCountResponse`가 적절 | `[코드]` |
| M12 | `schedule/presentation/history/response/FulfillmentBoardResponse.java` | `Board`가 UI 개념. 게다가 중첩 레코드가 `Row`/`Cell`이라 **표 좌표계를 응답 계약에 그대로 노출**. 매퍼도 `MeasurementHistoryMapper:34,36`의 `toRowResponse`/`toCellResponse` | `[판단]` |
| M13 | `contract/application/event/WorkplaceDeletedEventListener.java:3` | `client_management.application.event.WorkplaceDeletedEvent` 참조 — `port/in`이 아님. 모듈 간 이벤트를 계약으로 쓴다면 공개 위치로 승격해야 함 (G6 참조) | `[판단]` |

> `contract/infrastructure/entity/ContractTableViewEntity`의 `TableView`는 `@Subselect` **DB 뷰**를 뜻하는 read-model 엔티티명이므로 규칙 대상 밖입니다. 이름만 보면 오해 소지가 있다는 점만 기록합니다.

---

## 4. 경미 — 컨벤션 이탈

### 접미사 체계 밖의 VO `[판단]`

규칙 7의 접미사 표(`~Result`/`~ListItem`/`~Detail`/`~Summary`)에 없는 명명입니다. 규칙에 종류를 추가하거나 기존 접미사로 흡수하는 결정이 필요합니다.

- `schedule/application/command/export/*ExportView.java` (16개) — `~View` 접미사가 표에 없음
- `schedule/application/command/StackData.java` — 무접미사, `command/` 직하
- `schedule/application/command/detail/PreviousSheetCandidate.java` — `detail/` 하위인데 `~Detail` 아님
- `schedule/application/command/event/{SheetsSavedEvent, EditorRef}.java`
- `schedule/application/port/in/MonthlyMeasurementCount.java` — **port/in 공개 VO인데 `~Summary` 아님**
- `dashboard/application/command/{DashboardOverview, ExpiringContract, InspectionDue, MeasurementCountItem}.java` — `MeasurementCountItem`은 `~ListItem`이 규칙에 맞음
- `platform/application/command/TenantAdminCommand.java` — `{동작}{대상}Command` 프리픽스 규칙 불합치 (`ProvisionTenantCommand` 안에 중첩되는 값 객체 성격)

### 서비스·어셈블러 네이밍 `[판단]`

규칙 7이 정의한 것은 `{도메인}Service`와 `{도메인}DetailAssembler` 둘뿐입니다.

- `schedule/application/service/` — `AnalysisRecordIndexer`, `MeasurementRecordRecorder`, `PreviousSheetFinder`, `SnapshotSheetRecalculator`, `ScheduleStatusRecorder`
- `~Assembler`에 `Detail` 누락 — `schedule`의 `FulfillmentBoardAssembler`·`ScheduleExportAssembler`·`ScheduleSnapshotAssembler`, `client_management`의 `TeamAssembler`·`PollutantCatalogAssembler`
- 어셈블러 배치 불일치 — `client_management`는 `service/assembler/` 하위인데 `TeamAssembler`만 `service/` 직속. `contract/application/service/ContractDetailAssembler`도 `service/` 직속
- `platform/application/service/PlatformService` — 도메인이 `Tenant`이므로 규칙상 `TenantService`. 다만 `platform/.claude/CLAUDE.md:30`에 "빈 이름(`tenantService`) 충돌 회피"라는 근거가 있음 → 루트 규칙에 예외를 반영하는 쪽이 맞음

### 매퍼 `[판단]` / `[코드]`

- **`application/mapper/` 용도 이탈 3건** — 규칙 3은 이 위치를 "인터모듈 매퍼(타 모듈 port/in DTO ↔ 자기 도메인)" 전용으로 규정합니다. `schedule/application/mapper/`의 `ScheduleExportViewMapper`·`SheetExportViewMapper`·`ScheduleSnapshotMapper`는 모두 자기 모듈 내부 변환입니다. (부합하는 예: `admin/MemberPortMapper`, `dashboard/{ContractPortMapper, EquipmentPortMapper}`, `contract/ContractSummaryMapper`, `equipment/InspectionDueSummaryMapper`, `platform/TenantSummaryMapper`)
- **MapStruct 미사용 2건** — 위 `ScheduleExportViewMapper`·`SheetExportViewMapper`는 `@Component` 순수 Java입니다. 후자의 javadoc에 "하위 뷰는 소스가 null이어도 빈 인스턴스를 반환"이라는 jxls 템플릿 요구사항이 근거로 적혀 있어 정당성은 있으나 규칙 3에 예외가 없습니다.
- **메서드명** — `auth/presentation/mapper/SignUpRequestMapper.java:13`, `SignInRequestMapper.java:15`의 `toCommand()`. 규칙은 "단일 Command만 존재하는 도메인은 `toCreateCommand()` 형태" `[코드]`
- **메서드명** — `admin/presentation/mapper/MemberMapper.java:22, 24`의 `toCreateMemberCommand()` / `toUpdateMemberCommand()`. 규칙 명은 `toCreateCommand()` / `toUpdateCommand()`로 대상명을 넣지 않음 `[코드]`
- **`contract/infrastructure/mapper/ContractListItemMapper`** — infrastructure 매퍼는 `{도메인}EntityMapper` 규칙 (같은 패키지에 `ContractEntityMapper`가 별도 존재) `[판단]`

### 기타

- `client_management/application/validator/{FacilityValidator, PreventionValidator}` — `@RequiredArgsConstructor` 미부착. 의존성 0개라 기능상 무해하나 규칙 10 문구("`@Component` + `@RequiredArgsConstructor`로 선언")와 불일치 `[코드]`
- `admin/presentation/controller/MemberController.java:34, 64` — 반환 타입은 `ResponseEntity<ApiResponse<MemberResponse>>`인데 실제로는 본문 없는 `ApiResponse.success()`를 반환. **Swagger 스키마가 실제 응답과 어긋남** `[코드]`
- `schedule/domain/sheet/SheetMerge.java:94` — `new CustomException(ErrorCode.VALUE_MUST_NOT_BE_NULL, "측정 시트의 측정 카테고리는 필수 값입니다.")` 인라인 override. 규칙 12는 "먼저 도메인 특화 ErrorCode 추가 여부를 검토"라고 하므로 `SCHEDULE_SHEET_CATEGORY_REQUIRED` 신설이 적합. (같은 파일 72-75행은 시트 이름을 동적 삽입하므로 override 정당) `[코드]`
- `client_management/presentation/pollutant_catalog/controller/PlatformPollutantCatalogController` — `{도메인}Controller`에 `Platform` 접두. 모듈 문서 152-154행에 "클래스는 이 모듈, URL만 `/api/platform/...`"이라는 근거 있음 `[문서]`
- `admin/presentation/controller/DocumentManagementController` — `storage`의 `DocumentController`와 빈 이름 충돌 회피 목적으로 보임. 근거 문서 없음 `[문서]`
- 빈 디렉터리 5개 — `admin/application/port/in`, `admin/application/port/out`, `admin/infrastructure`, `auth/application/mapper`, `contract/.claude` `[코드]`

---

## 5. 문서 드리프트 — 코드가 맞고 문서가 틀린 것 `[문서]`

### 5-1. `tenant` 모듈은 존재하지 않는다

현재 모듈은 **admin · auth · client_management · contract · dashboard · equipment · global · platform · schedule · storage** 10개입니다. 과거 `tenant`가 `client_management`(테넌트 내부 업무)와 `platform`(테넌트 생명주기)으로 분리·개명된 것으로 보이며, `TenantEntity`/`TenantJpaRepository`도 현재는 `platform/infrastructure/`에 있습니다.

문서 7곳이 사라진 모듈을 가리킵니다.

| 파일 | 위치 | 잘못된 서술 | 실제 |
|---|---|---|---|
| `CLAUDE.md` | §10 말미 | "레퍼런스 구현은 `tenant/application/validator/`입니다" | `client_management/application/validator/` (Validator 9개) |
| `CLAUDE.md` | §8 Command 하위 패키지 | "(tenant 모듈 참고)" | `client_management` (`command/create·update·detail·list_item/`) |
| `ARCHITECTURE.md` | 4-10행 | 모듈 목록이 auth·client_management·contract·global 4개 | 실제 10개 |
| `ARCHITECTURE.md` | 246-258행 | "Tenant 애그리거트의 소유권" 절 전체가 `tenant` 모듈 존재를 전제 | `platform`이 도메인·포트·어댑터·엔티티 전부 소유 |
| `ARCHITECTURE.md` | 236, 240-241행 | "`auth`/`contract` → `tenant`", "`tenant`는 중심 모듈이며 `platform`을 알지 못합니다" | 대상 부재 |
| `platform/.claude/CLAUDE.md` | 5행 | "테넌트 내부 업무를 다루는 `tenant` 모듈과 관심사가 분리된다" | `client_management`로 대체 필요 |
| `platform/.claude/CLAUDE.md` | 13-14행 | "`TenantEntity`/`TenantJpaRepository`는 `tenant/infrastructure`에 그대로 둔다. 본 모듈의 어댑터가 이를 재사용한다" | 둘 다 `platform/infrastructure/`에 있음 — 재사용 관계 자체가 없음 |

추가로 `platform/.claude/CLAUDE.md:31`은 "`application/port/in/TenantQueryUseCase`·`TenantSummary` — 타 모듈 공개 계약"이라고 적었으나 **`TenantSummary`는 실제로 `application/result/`에 있습니다**(H4). 문서가 옳고 코드가 틀린 케이스입니다.

### 5-2. 기술 스택 누락

루트 `CLAUDE.md`의 "기술 스택" 절에 없는데 `build.gradle`에 있는 것:

| 누락 항목 | 위치 | 실사용처 |
|---|---|---|
| **Spring Data MongoDB** | `build.gradle:29` | `equipment`(컬렉션 `equipments`, `equipment_inspection_records`), `schedule`(`analysis_records`, `schedule_documents`) |
| **jxls-poi 3.1.0** | `build.gradle:53` | `schedule/infrastructure/excel/` — 엑셀 성적서 출력 |

MongoDB는 `equipment/.claude/CLAUDE.md`에만 적혀 있어, **루트 문서만 읽으면 MySQL 전용 프로젝트로 오해**합니다. equipment·schedule 두 모듈이 Mongo 기반인 만큼 루트 스택에 반영이 필요합니다.

### 5-3. 규칙 표가 현실과 어긋남

| 대상 | 내용 |
|---|---|
| `CLAUDE.md` §7 Repository Port 메서드 표 | `findById(Long id)`로 적혀 있으나 실제로는 멀티테넌시 때문에 `findById(Long id, Long tenantId)`가 13개 포트에 일관 적용. 표에 tenant 파라미터를 반영해야 신규 작업자가 헷갈리지 않음 |
| `CLAUDE.md` §6 | "**모든** 엔드포인트는 `ApiResponse<T>`를 반환합니다"라고 단정하나, 바이너리·SSE 예외 4곳이 코드 javadoc에만 근거를 둠 — `storage/.../DocumentController:78,87`(`ResponseEntity<byte[]>`), `schedule/.../ScheduleExportController:47,63`(동일), `schedule/.../ScheduleStreamController:45`(`SseEmitter`). 규칙 본문에 예외를 명문화하는 편이 안전 |
| `client_management/.claude/CLAUDE.md` | Validator 표 74-76행 — `requireMemberInTenant`가 `PreventionValidator` 행 그룹에 붙어 있으나 실제로는 **`TeamValidator` 소속**. 표 렌더링상 버그 |

### 5-4. `docs/DATABASE.md`

| 항목 | 내용 |
|---|---|
| 누락 테이블 | `teams`(client_management), `documents`·`document_versions`(storage) — 전용 섹션 없음. `teams`는 639행에 Auditing 관련으로만 스쳐 언급 |
| 제목 불일치 | `## contract — 계약` 섹션 제목이 실제 테이블명 `contracts`와 다름 (다른 섹션은 전부 실제 테이블명 사용) |

---

## 6. 문서 공백 — 규칙 자체가 없는 것 `[문서]`

| # | 공백 | 근거 |
|---|---|---|
| G1 | **`schedule` 모듈 CLAUDE.md 없음** | 163파일로 `client_management`(166) 다음 규모인데 모듈 문서 0. 그 결과 `application/calculation/`(시트 계산 파이프라인 13파일), `~ExportView` 16개, equipment 도메인 결합(H2), presentation 이중 구조(M4), Recorder·Finder·Indexer 계열 서비스가 **전부 근거 없이** 존재. 이번 점검에서 나온 중간·경미 항목의 절반가량이 이 모듈에 몰려 있음 |
| G2 | auth(51) · storage(31) · contract(27) · admin(14) 모듈 문서 없음 | `contract/.claude/`는 디렉터리만 만들어 두고 **비어 있음**. **§1 치명 3건이 나온 모듈이 정확히 `auth`·`admin`·`contract` — 모듈 문서가 없는 모듈들입니다.** 테넌트 격리 규칙이 `client_management/.claude/CLAUDE.md`에만 적혀 있어 이 세 모듈이 규칙의 사정권 밖에 있었던 것이 원인으로 보입니다 |
| G3 | **테스트 규칙 없음** | `src/test`에 42개 파일이 있고 `.claude/agents/test-writer.md`·`.claude/commands/test-push.md`까지 갖췄는데 루트 `CLAUDE.md`에 테스트 관련 항목이 전무. 테스트 보유 모듈은 client_management·equipment·schedule 3개뿐이며 auth·contract·platform·storage·admin·dashboard는 0. Fake 리포지토리 패턴(`FakePollutantRepository` 등)이 사실상 표준으로 쓰이는데 문서화되지 않음 |
| G4 | **`port/in` 도메인 타입 노출 예외 조항 없음** | `equipment/.claude/CLAUDE.md:91`이 명시적으로 선언하고 소비 모듈 12곳이 `equipment.domain`을 import하는데, 루트 규칙 1에 예외가 없어 **형식상 전부 위반으로 읽힘** (H2 참조) |
| G5 | **`global/common/enums/` 근거 없음** | 규칙 2는 "global에 기능성 코드를 추가하지 않습니다. global은 공통 인프라만 관리합니다"인데 도메인 enum 10개가 있음. 다만 실제 사용처를 보면 `Grade`·`MeasurementField`·`Shape`·`Orientation`·`MeasurementMethod`·`MeasurementCycle`·`PollutantPhase` 7개는 client_management+schedule 공유, `DocumentCategory`는 admin+storage 공유 → **공유 커널로 정당화 가능**. 규칙 2에 예외 문구를 넣는 쪽이 맞음 (`SubscriptionPlan`·`TenantStatus`는 platform 전용이므로 모듈로 내리는 것을 검토) |
| G6 | **이벤트 패키지 규칙 없음** | `client_management/application/event/`, `contract/application/event/`, `schedule/application/command/event/` 세 가지 위치로 분산. 모듈 간 이벤트가 공개 계약인지(→ `port/in`) 내부 구현인지 규정이 없어 M13 같은 경계 모호함이 발생 |
| G7 | **`{도메인}Service` 예외 근거 없음** | `PlatformService`(빈 이름 충돌 회피), schedule의 Recorder·Finder·Indexer 계열이 모듈 문서에만 있거나 아예 무근거 |

---

## 7. 준수 확인 — 전수 검색 결과 위반 0건

무엇이 잘 지켜지고 있는지도 기록합니다. 아래는 `src/main` 569개 + `src/test` 42개 파일 전수 검색 결과입니다.

| 규칙 | 결과 |
|---|---|
| presentation → infrastructure | **0건** (FQCN·와일드카드 우회 포함) |
| application → infrastructure | **0건** |
| domain → JPA(`jakarta.persistence`) | **0건** |
| domain → `jakarta.validation` | **0건** |
| domain → infrastructure / presentation | **0건** |
| application·presentation에서 `*JpaRepository`·`*Entity` 직접 사용 | **0건** (§2 H1의 `global` 1건 제외) |
| `@Autowired` / `@Inject` / `@Resource` 필드 주입 | **0건** — 규칙 5 완전 준수 |
| Command 프리픽스 순서 `{동작}{대상}` | 33개 전부 준수, 역순(`UserCreateCommand` 형태) 0건 |
| Swagger `@Tag` | 컨트롤러 24/24 준수 |
| Validator 규칙 (규칙 10) | 12개 전부 준수 — 클래스명 `{애그리거트}Validator`, 위치 `application/validator/`, `require*` 접두(`validate*` **0건**), `@Transactional` 미부착, port만 주입(JpaRepository 0건), `void` 반환 |
| ErrorCode (규칙 12) | 55개 중 도메인 특화 48개 전부 `{DOMAIN}_{CONDITION}` prefix 보유. 범용 7개도 주석으로 분리. `GlobalExceptionHandler`의 4xx/5xx 로그 레벨 분기, `UNAUTHORIZED` vs `BadCredentialsException` 분리(`CustomAuthenticationFailureHandler`)도 규정대로 |
| 매퍼 위치 (규칙 3) | 55개 전부 `presentation/**/mapper/`(19) · `infrastructure/mapper/`(18) · `application/mapper/`(8) 중 하나. 그 외 경로 0건 |
| 컨트롤러·서비스의 수동 매핑 | `new XxxResponse(...)`·`new XxxCommand(...)` 직접 생성 **0건** |

`client_management`는 규칙 4(포트 위치)·규칙 10(Validator)의 레퍼런스로 지목된 대로 실제로 정합하며, 이번 점검에서 구조적 위반이 나오지 않은 유일한 대형 모듈입니다.

---

## 8. 권장 조치 순서

판단은 사용자 몫이므로 비용 대비 효과 순으로 제시만 합니다. 1~3번은 아키텍처 정비가 아니라 **보안 조치**이므로 나머지와 성격이 다릅니다.

| 순위 | 항목 | 규모 | 해소되는 것 |
|---|---|---|---|
| ~~1~~ | ~~공개 sign-up의 `tenantId`·`roleId` 수용 차단~~ (§1-A) | ✅ **2026-08-25 완료** — 엔드포인트 제거 | 인증 없는 플랫폼 운영자 권한 획득 |
| ~~2~~ | ~~회원 단건 경로 테넌트 격리 + 역할 부여 제한~~ (§1-B) | ✅ **2026-08-25 완료** | 교차 테넌트 계정 조작, ADMIN 자기 승격 |
| ~~3~~ | ~~contract 테넌트 격리~~ (§1-C) | ✅ **2026-08-25 완료** — 단건·목록·생성 경로 | 교차 테넌트 계약 노출·조작 |
| 4 | `platform/application/result/` 해체 (H4) | 2파일 이동 + import 10곳 | 규칙 8 위반 + 모듈 경계 위반 + `platform/.claude/CLAUDE.md:31` 문서 불일치 |
| 5 | `port/` 직하 2건 이동 (M1·M2) | 2파일 + import 3곳 | 규칙 4 포트 위치 표준화 |
| 6 | `AuthController.signup` 반환 타입 (M11), `UserRepository`의 `@Repository` 제거 (M9) | 각 1파일 | 규칙 6·규칙 1 |
| 7 | 문서의 `tenant` → `client_management`/`platform` 갱신 + 기술 스택에 MongoDB·jxls 추가 (§5-1, §5-2) | `CLAUDE.md`, `ARCHITECTURE.md`, `platform/.claude/CLAUDE.md` | 신규 작업자가 존재하지 않는 경로를 찾아 헤매는 문제 |
| 8 | `CustomUserDetailsService`를 port/in 경유로 전환 (H1) | 1파일 + auth port/in 확장 | 유일하게 뚫린 모듈 경계 |
| 9 | **`schedule` 모듈 CLAUDE.md 신규 작성** (G1) | 신규 1파일 | H2·M4 및 경미 항목 다수에 근거 부여 |
| 10 | `domain/port/` → `application/port/out/` 이관 (M3) | auth 6 + contract 1 + 어댑터·서비스 import | 규칙 4 잔여 이관 완료. H3도 동시 해소 |
| 11 | 규칙 자체 보강 — 공유 커널 예외(G4·G5), 테스트 규칙(G3), 이벤트 위치(G6), `ApiResponse` 예외(§5-3) | 문서만 | 현재 "형식상 위반"으로 잡히는 정상 코드들을 정리 |

> **2번과 10번은 함께 하는 편이 낫습니다.** `UserQueryUseCase`/`UserCommandUseCase`에 `tenantId`를 추가하면 `auth/domain/port/UserRepository`까지 시그니처가 전파되는데, 어차피 그 파일을 `application/port/out/`으로 옮기고 `@Repository`·`Optional`을 정리해야 하기 때문입니다(M3·M7·M9).

> **테넌트 격리 규칙을 루트 `CLAUDE.md`로 올리는 것을 권합니다.** 현재 이 규칙은 `client_management/.claude/CLAUDE.md`에만 있어 모듈 문서가 없는 `auth`·`admin`·`contract`가 전부 빠졌습니다. 위 1~3번이 세 모듈에서 동시에 나온 것은 우연이 아닙니다 — **모듈 문서 부재(G2)가 곧 보안 결함으로 이어진 사례**입니다.

### 이 리포트가 다루지 않은 것

- **전면적인 보안 점검** — §1의 3건은 *테넌트 격리 규칙 준수 여부*를 확인하다 드러난 것이지, 보안 감사를 수행한 결과가 아닙니다. 인증·인가 경로 전반(토큰 수명·회전, CORS, 쿠키 속성, 입력 검증, 파일 업로드 등)은 살펴보지 않았으므로 **§1이 취약점의 전부라고 볼 수 없습니다.** `/security-review` 등으로 별도 점검을 권합니다
- 성능·쿼리 최적화 (N+1, 인덱스 등) — `dashboard/.claude/CLAUDE.md`의 "향후 과제"에 별도로 정리돼 있습니다
- 비즈니스 로직 정확성 — 규칙 준수 여부만 봤습니다
- 커밋되지 않은 199개 변경분의 완성도 — 워킹트리 상태 그대로 규칙 관점에서만 점검했습니다

### 점검 방법

- `src/main/java` 569개 + `src/test` 42개 파일 대상 전수 grep (FQCN·와일드카드 우회 경로 포함)
- 탐색은 병렬 에이전트 3개(구조·레이어 의존성·네이밍/검증)로 수행하고, **치명·높음 등급과 주요 중간 등급 항목은 원본 파일을 직접 열어 재확인**했습니다
- 라인 번호는 2026-08-24 시점 워킹트리 기준입니다
