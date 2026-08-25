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
- **`UserCommandUseCase.createPlatformAdmin`이 이 제한을 받지 않는 유일한 경로**입니다.
  부트스트랩 전용이며, 이 예외가 없으면 서버 최초 기동이 `ROLE_NOT_ASSIGNABLE`로 실패합니다.

> **"역할이 존재하는가"는 권한 검증이 아닙니다.** `roleRepository.findById(roleId)`가 통과한다는 것은
> 그 역할이 DB에 있다는 뜻일 뿐입니다. 이 둘을 섞지 마세요.

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

## 타 모듈 공개 계약 (`application/port/in`)

| 계약 | 구현체 | 소비 모듈 |
|---|---|---|
| `UserQueryUseCase` — `getUser(userId, tenantId)`, `getUserList(tenantId)`, `existsByUsername` | `UserService` | `admin`, `client_management`(팀 사수·부사수 이름), `global`(예정) |
| `UserCommandUseCase` — `createUser`, `updateUser`, `deleteUser`, `createPlatformAdmin` | `AuthService` | `admin`, `platform` |
| `RoleQueryUseCase` / `RoleCommandUseCase` | `RoleService` | `platform`(부트스트랩 역할 확보) |

공개 VO는 `UserSummary`(`port/in`)이며 비밀번호를 담지 않습니다.
`CreateUserCommand`·`UpdateUserCommand`도 공개 계약이라 `port/in`에 둡니다.

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

### 포트 위치 — `domain/port/` 잔존

이 모듈은 **아웃바운드 포트 6개가 아직 `domain/port/`에 있습니다.**
`Authenticator` · `PasswordEncryptor` · `RoleRepository` · `TokenIssuer` · `TokenParser` · `UserRepository`

표준 위치는 `application/port/out/`이며(루트 규칙 4) 향후 이관 예정입니다.
**신규 포트는 `application/port/out/`에 만듭니다** — `RefreshTokenStore`가 이미 그렇습니다.

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

- `domain/port/` 6개를 `application/port/out/`으로 이관 (루트 규칙 4)
- `AuthService.register`의 아이디 중복 검사가 `IllegalArgumentException`을 던집니다.
  `USER_USERNAME_DUPLICATED` 같은 도메인 ErrorCode로 교체해야 규칙 12에 맞습니다
- `GET /api/roles`가 `PLATFORM_ADMIN`을 포함한 전체 역할을 인증된 모든 사용자에게 노출합니다.
  부여 자체는 막혀 있으나 목록 노출이 필요한지 재검토
- 이 모듈에는 테스트가 없습니다. 역할 부여 제한과 tenant 격리는 회귀가 곧 취약점이므로 우선순위가 높습니다
