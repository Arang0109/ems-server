# auth 모듈 가이드라인

인증·인가의 원장 모듈입니다. 사용자·역할을 소유하고, 로그인과 토큰 발급·재발급을 담당하며,
사용자 정보가 필요한 다른 모듈에 `port/in` 계약을 공개합니다.

---

## 애그리거트

| 애그리거트 | 저장소 | 도메인 루트 | 비고 |
|---|---|---|---|
| **User** | MySQL `users` | `domain/User` | tenant 종속. `tenant_id`는 plain `Long` 컬럼(JPA 연관 없음) |
| **Role** | MySQL `roles` (+ `privileges`, `role_privileges`) | `domain/Role` | **전역 마스터** — tenant에 종속되지 않음 |

`AuthenticatedUser`·`TokenResult`는 애그리거트가 아니라 인증 절차가 주고받는 값 객체입니다.

---

## 공개 가입을 두지 않는다

**`POST /api/auth/sign-up`은 존재하지 않습니다.** 삭제된 것이며 되살리지 않습니다.

계정이 만들어지는 경로는 둘뿐입니다.

| 경로 | 담당 | 만드는 것 |
|---|---|---|
| `POST /api/admin/members` | `admin` 모듈 (ADMIN 전용) | 테넌트 내부 회원 |
| `platform`의 `provisionTenant` | `platform` 모듈 (PLATFORM_ADMIN 전용) | 테넌트 발급과 함께 그 테넌트의 최초 ADMIN |
| `PlatformAdminInitializer` | 부트스트랩 (배포 1회성) | 플랫폼 운영자 계정 |

공개 가입은 **누가 어느 테넌트에 어떤 역할로 들어오는지를 요청자가 정하게 만듭니다.**
그 값을 검증할 근거가 서버에 없으므로 엔드포인트 자체를 두지 않습니다.
가입 코드·초대 토큰 같은 서버 주도 방식을 도입하기 전에는 다시 열지 않습니다.

---

## 사용자 조회 경로가 둘인 이유

같은 원장(`User`)을 보지만 **노출 범위가 달라** 엔드포인트를 나눕니다. 하나로 합치지 마세요.

| 경로 | 권한 | 응답 | 용도 |
|---|---|---|---|
| `GET /api/admin/members` | ADMIN 전용 | `MemberResponse` — 로그인 아이디·이메일·연락처·tenantId 포함 | 회원 관리 화면(생성·수정·삭제와 한 세트) |
| `GET /api/users` | 인증된 사용자 전원 | `UserListResponse` — `userId`·`name`·`department`·`role`만 | 선택지 드롭다운 (측정계획 담당자 배정 등) |

- 조회 자체는 `UserService.getUserList(tenantId)` 하나를 공유합니다. 갈라지는 것은 **응답 DTO**뿐입니다.
- `UserMapper`(auth presentation)에 `unmappedTargetPolicy = ERROR`를 걸지 않은 것은 의도입니다.
  `UserListResponse`는 `UserSummary`의 부분집합이며, **공개 VO에 필드를 더했다고 이 응답이 자동으로
  넓어져서는 안 되기** 때문입니다. `admin`의 `MemberMapper`는 반대로 ERROR라 필드 추가 시 컴파일이 깨집니다 —
  두 매퍼의 정책 차이가 곧 "누구에게 보여도 되는가"의 경계입니다.
- `/api/users`에 필드를 더할 때는 **권한 없는 사용자에게 보여도 되는 값인지**를 먼저 따집니다.

---

## 역할 부여 제한

테넌트 격리만으로는 **자기 계정 권한 상승**을 막지 못합니다. 격리를 지켜도 "내 계정의 역할을
`PLATFORM_ADMIN`으로 바꾸는" 요청은 여전히 자기 테넌트 안의 정상 요청이기 때문입니다.

- `application/validator/UserValidator.requireAssignableRole(roleId)`
  - 존재하지 않는 역할 → `ROLE_NOT_FOUND` (Adapter가 던짐)
  - `PLATFORM_ADMIN` → `ROLE_NOT_ASSIGNABLE` (403)
  - `roleId == null`이면 역할 변경이 없는 것으로 보고 통과
- **`createUser`와 `updateUser` 양쪽에 적용합니다.** 수정 경로만 막으면 관리자가 운영자 계정을
  **새로 만들** 수 있습니다.
- 판정 자체는 도메인이 소유합니다 — `Role.PLATFORM_ADMIN` 상수 + `Role.isPlatformAdmin()`.
- **선택지에서도 빠집니다** — `GET /api/roles`(`RoleService.getAssignableRoles`)가 `PLATFORM_ADMIN`을 제외합니다.
  부여를 막는 것과 목록에서 빼는 것은 층이 다릅니다: 앞은 보안 경계라 서버에만 둘 수 있고,
  뒤는 UX이지만 **역시 서버가 합니다.** 클라이언트가 거르면 같은 규칙이 클라이언트 수만큼 복제되고,
  서버는 "고를 수 없는 것을 선택지로 주는" 상태로 남습니다. 두 경로 모두 `Role.isPlatformAdmin()`을 봅니다.
  회귀 방지는 `RoleServiceTest`·`UserValidatorTest`·`AuthServiceTest`가 맡습니다
- **`UserCommandUseCase.createPlatformAdmin`이 이 제한을 받지 않는 유일한 경로**입니다.
  부트스트랩 전용이며, 이 예외가 없으면 서버 최초 기동이 `ROLE_NOT_ASSIGNABLE`로 실패합니다.

> **"역할이 존재하는가"는 권한 검증이 아닙니다.** `roleRepository.findById(roleId)`가 통과한다는 것은
> 그 역할이 DB에 있다는 뜻일 뿐입니다. 이 둘을 섞지 마세요.

---

## 인증 주체 조회 — `global`이 auth를 들여다보지 않는다

스프링 시큐리티의 `UserDetailsService`(`global/security/user/CustomUserDetailsService`)는
**`UserCredentialQueryUseCase` 하나만** 봅니다. 한때 이 클래스가 `UserEntity`·`RoleEntity`·
`UserJpaRepository`를 직접 들고 있었고, 진단 리포트에 세 번 연속 이월된 유일한 모듈 경계 위반이었습니다.

- `UserCredentialSummary`는 **암호화된 비밀번호를 담는 유일한 공개 VO**입니다. 그래서 일반 조회
  계약(`UserQueryUseCase`)과 나눠 두었습니다 — 비밀번호가 필요한 곳은 시큐리티 하나뿐입니다.
- **테넌트 이름은 이 VO에 없습니다.** 그것은 `platform`의 원장이고, auth가 조회하면
  `platform → auth` 의존과 맞물려 순환이 됩니다. 두 모듈의 값을 합치는 일은 `global`이 합니다.
- 그 순환은 실제로 한 번 발생했습니다. 지금은 세 가지로 끊겨 있습니다 —
  `PasswordEncoder` 빈을 `SecurityConfig`에서 분리(`PasswordEncoderConfig`),
  `platform`의 조회 계약 구현 분리(`TenantQueryService`), 그리고 위의 "auth는 테넌트를 모른다"는 규칙.
  **셋 중 어느 하나라도 되돌리면 기동이 실패합니다.**

---

## 토큰

### 발급

로그인 시 Access Token(응답 본문)과 Refresh Token(HttpOnly 쿠키)을 함께 내립니다.
Refresh Token은 Redis(`RefreshTokenStore`)에도 저장합니다.

`SignInResult`에는 소속 팀(`teamId`·`teamName`)이 포함됩니다. 이 값은 `client_management`의
`TeamQueryUseCase`로 **로그인 시점에 1회만** 조회합니다 — 인증 principal에 넣지 않는 이유는
매 요청마다 팀을 읽을 이유가 없기 때문입니다.

### 재발급 — Refresh Token을 회전시키지 않는다

`RefreshTokenService.reissueAccessToken`은 Refresh Token을 새로 발급하지 않습니다.

프론트는 Access Token이 만료된 요청 **여러 건을 동시에** 재시도합니다. 회전시키면 그중 하나만
성공하고 나머지는 이미 폐기된 토큰을 들고 실패해 사용자가 로그아웃됩니다.
대신 **저장소의 값과 정확히 일치할 때만** 재발급합니다.

검증 순서에 의미가 있습니다.

1. **서명·만료**(JWT 자체 검증) — `TokenParser.extractUsername`
2. **저장소 대조** — 로그아웃 시 삭제되므로 이것이 강제 만료 수단입니다
3. **계정 재조회** — 그 사이 바뀐 소속·권한을 새 Access Token에 반영합니다

어느 단계든 실패하면 전부 `REFRESH_TOKEN_INVALID`입니다. 실패 이유를 구분해 알려주지 않습니다.

### 쿠키 속성은 설정에서 읽는다

`secure`·`SameSite`는 배포 환경마다 다릅니다(`global/security/domain/AuthCookieProperties`).
HTTP로 접속하는 개발 서버에 secure 쿠키를 내려보내면 **브라우저가 조용히 버려** 재발급이 영영 실패합니다.

---

## 경로 보호 (`SecurityConfig`)

| 경로 | 요구 권한 |
|---|---|
| `/api/platform/**` | `hasRole("PLATFORM_ADMIN")` |
| `/api/admin/**` | `hasRole("ADMIN")` |
| `/api/auth/sign-in`, `/api/auth/refresh`, Swagger | permitAll |
| 그 외 전부 | `authenticated()` |

- `PLATFORM_ADMIN`은 계층상 `ADMIN`의 상위입니다(`ROLE_PLATFORM_ADMIN > ROLE_ADMIN`).
- **SSE의 ASYNC 재디스패치와 ERROR 디스패치는 permitAll입니다.** 최초 REQUEST에서 이미 인가를
  통과했고 그 시점 `SecurityContext`는 비어 있어, 다시 인가하면 Access Denied가 됩니다.
- 인증 실패(401)와 인가 실패(403)는 다른 핸들러가 처리합니다.
  로그인 실패는 `CustomAuthenticationFailureHandler`가 맡으며 `UNAUTHORIZED`와 섞지 않습니다
  (루트 `CLAUDE.md` 규칙 12).

---

## 사용자 조회 — 관리 화면과 나눕니다

| 경로 | 컨트롤러 | 권한 | 응답 |
|---|---|---|---|
| `GET /api/users` | `auth`의 `UserController` | `authenticated()` | `UserListResponse` — `userId`·`name`·`department`·`role` |
| `GET /api/admin/members` | `admin`의 `MemberController` | `hasRole("ADMIN")` | `MemberResponse` — 전체 필드 |

**같은 원장을 두 경로로 여는 것은 소비자와 필드 범위가 다르기 때문입니다.**
측정계획 등록처럼 사람을 고르는 화면은 역할과 무관하게 목록이 필요하지만 이름·부서·역할이면 충분하고,
회원 관리 화면은 ADMIN만 보되 로그인 아이디·연락처까지 필요합니다.
하나로 합치면 둘 중 하나가 **과한 권한이거나 과한 노출**이 됩니다.

- `/api/users`에서 빠지는 것: `username`(로그인 아이디)·`email`·`tel`·`roleId`·`tenantId`.
  이 제외가 엔드포인트의 존재 이유이므로 `UserListResponseMappingTest`가 응답 필드 목록 자체를 고정합니다.
  필드를 더하기 전에 "같은 테넌트의 모든 사용자가 봐도 되는가"를 먼저 답하세요.
- **`/api/users`는 조회만 갖습니다.** 여기에 생성·수정·삭제를 더하면 "공개 가입을 두지 않는다"가 무너집니다.
- `SecurityConfig`에는 규칙을 추가하지 않았습니다 — `anyRequest().authenticated()`에 걸립니다.
- 컨트롤러가 `UserQueryUseCase`가 아니라 `UserService`를 직접 주입합니다.
  타 모듈 소비자가 없는 경로라 UseCase 인터페이스를 두지 않는다는 루트 규칙이며, `RoleController`와 같습니다.

---

## 타 모듈 공개 계약 (`application/port/in`)

| 계약 | 구현체 | 소비 모듈 |
|---|---|---|
| `UserQueryUseCase` — `getUser(userId, tenantId)`, `getUserList(tenantId)`, `existsByUsername` | `UserService` | `admin`, `client_management`(팀 사수·부사수 이름), `schedule`(회차별 측정자 검증·이름) |
| `UserCommandUseCase` — `createUser`, `updateUser`, `deleteUser`, `createPlatformAdmin` | `AuthService` | `admin`, `platform` |
| `RoleQueryUseCase` / `RoleCommandUseCase` | `RoleService` | `platform`(부트스트랩 역할 확보) |
| `UserCredentialQueryUseCase` — `findCredentialByUsername` | `UserService` | `global`(스프링 시큐리티 `UserDetailsService`) |

공개 VO는 `UserSummary`(`port/in`)이며 비밀번호를 담지 않습니다.
`CreateUserCommand`·`UpdateUserCommand`도 공개 계약이라 `port/in`에 둡니다.

**이 세 타입은 소비 모듈이 감싸지 않고 그대로 씁니다**(루트 `CLAUDE.md`의 공유 커널). `admin`이 그 예로,
`MemberController`가 Request를 `CreateUserCommand`로 곧장 바꾸고 `UserSummary`를 `MemberResponse`로 옮깁니다.
중간 도메인·커맨드를 두었다가 2026-09-06에 걷어냈습니다 — 필드가 같은 타입을 한 겹 더 두면 변환만 늘고,
필드를 더할 때 양쪽을 고쳐야 했기 때문입니다.

> **그래서 이 세 타입의 필드를 바꿀 때는 소비 모듈의 응답 계약을 함께 봅니다.** `admin`의 `MemberMapper`는
> `unmappedTargetPolicy = ERROR`라 필드를 더하면 그쪽 컴파일이 깨집니다. 그 자리에서 `MemberResponse`에
> 노출할지 결정하세요 — 조용히 누락되지 않게 하려고 일부러 그렇게 두었습니다.

> `getUser(userId, tenantId)`는 **tenant 범위 조회**입니다. 소비 모듈이 tenant를 따로 대조할 필요가 없고,
> 해서도 안 됩니다 — 대조 책임이 두 곳으로 갈리면 한쪽이 빠집니다.

---

## 모듈 규칙

### tenant 소유권 격리

루트 `CLAUDE.md` 규칙 13을 따릅니다. 이 모듈에서 주의할 점:

- **`Role`은 전역 마스터**라 `RoleRepository`가 tenantId를 받지 않습니다. 의도된 예외입니다.
- `existsByUsername(String)`도 tenant를 받지 않습니다 — **아이디는 전 테넌트 전역 유일**입니다
  (로그인 시 테넌트를 지정하지 않으므로 username만으로 계정이 특정되어야 합니다).
- `User` 단건 조회·삭제는 `(userId, tenantId)`입니다. 소유권 불일치는 `USER_NOT_FOUND`(404)로 은닉합니다.

### 포트 위치

아웃바운드 포트는 전부 `application/port/out/`에 있습니다. 한때 6개가 `domain/port/`에 남아
루트 규칙 4를 어기고 있었으나 **2026-09-06에 이관을 마쳤고**, 그 패키지는 더 이상 없습니다.

### `Optional` 반환 — 이 모듈의 예외

루트 규칙 7은 Port의 단건 조회가 `Optional`을 반환하지 않도록 합니다. 이 모듈에는 예외가 3건 있습니다.

| 위치 | 의도 |
|---|---|
| `UserRepository.findById(id, tenantId)` | 미존재와 타 tenant를 **같은 빈 값**으로 돌려 리소스 존재를 은닉합니다. 호출부가 `USER_NOT_FOUND`로 통일합니다 |
| `UserRepository.findByUsername` | 위와 동일 |
| `Authenticator.loadAuthenticatedUser` | 재발급 도중 계정이 사라진 경우를 예외가 아닌 정상 분기로 다룹니다 |

> `TokenParser.extractUsername`의 `Optional<String>`은 Repository 포트가 아니며,
> "서명이 맞지 않거나 만료된 토큰은 예외 대신 빈 값"이라는 의도가 javadoc에 있습니다.

### `client_management`와의 상호 참조

`AuthService`가 로그인 응답을 만들 때 `client_management`의 `TeamQueryUseCase`를 씁니다.
반대로 `client_management`의 `TeamAssembler`·`TeamValidator`는 이 모듈의 `UserQueryUseCase`를 씁니다.
**두 모듈이 서로의 `port/in`을 참조하는 유일한 쌍입니다.**

양쪽 모두 공개 계약만 참조하므로 규칙 위반은 아니지만, 계약을 넓힐 때는 순환이 더 깊어지지 않는지
확인하세요. 소속 팀 정보를 인증 principal이나 토큰에 넣으면 이 참조를 끊을 수 있습니다.

---

## 향후 과제

- `RoleService.getAssignableRoles`는 `PLATFORM_ADMIN`만 걸러냅니다. 앞으로 부여 불가 역할이 늘면
  판정을 `Role`로 옮겨 한 곳에서 관리해야 합니다
