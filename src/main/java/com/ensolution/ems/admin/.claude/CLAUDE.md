# admin 모듈 가이드라인

테넌트 **관리자(ADMIN)** 전용 관리 화면의 백엔드입니다. 회원 관리와 문서 관리 두 가지를 담습니다.
`/api/admin/**`는 `SecurityConfig`에서 `hasRole("ADMIN")`으로 보호됩니다.

---

## 자체 원장이 없는 모듈

**이 모듈은 자기 테이블을 갖지 않습니다.** `infrastructure/` 패키지 자체가 없고,
`port/out`도 없습니다. 데이터는 전부 다른 모듈의 `port/in`에서 옵니다.

| 관심사 | 원장 소유 모듈 | 이 모듈이 쓰는 계약 |
|---|---|---|
| 회원 | `auth` | `UserQueryUseCase`, `UserCommandUseCase` |
| 문서 | `storage` | `DocumentCommandUseCase` |

그래서 `MemberService`는 **유스케이스 조율만** 합니다 — 포트를 호출하고 매퍼로 변환할 뿐
자체 비즈니스 규칙이 없습니다. 규칙은 원장 모듈이 소유합니다(예: 역할 부여 제한은 `auth`의 `UserValidator`).

> **원장을 갖지 않는 모듈은 규칙을 중복 구현하지 않습니다.** 검증을 여기 옮겨 오면 원장 모듈의
> 다른 호출 경로가 그 검증을 우회하게 됩니다.

### `domain/Member`는 무엇인가

`Member`는 JPA 엔티티가 아니라 **auth의 `UserSummary`를 admin 쪽 언어로 옮긴 표현**입니다.
관리 화면이 "사용자"가 아니라 "회원"이라는 말을 쓰기 때문에 두며, 여기에 admin 고유 필드가
붙을 자리를 마련해 둡니다.

변환은 `application/mapper/MemberPortMapper`가 담당합니다 —
**루트 `CLAUDE.md` 규칙 3의 인터모듈 매퍼 레퍼런스**입니다.

| 방향 | 메서드 |
|---|---|
| auth `UserSummary` → admin `Member` | `toMember`, `toMemberList` |
| admin `CreateMemberCommand` → auth `CreateUserCommand` | `toCreateUserCommand` |
| admin `UpdateMemberCommand` → auth `UpdateUserCommand` | `toUpdateUserCommand` |

이 매퍼 덕분에 컨트롤러·서비스가 auth의 DTO를 직접 들고 다니지 않습니다.
auth가 `UserSummary` 형태를 바꿔도 이 파일 하나만 고치면 됩니다.

---

## 유스케이스

| 서비스 | 메서드 | 위임 대상 |
|---|---|---|
| `MemberService` | `createMember`, `getMember`, `getMemberList`, `updateMember`, `deleteMember` | auth `UserCommandUseCase` / `UserQueryUseCase` |

문서 관리는 서비스를 두지 않고 컨트롤러가 `storage`의 `DocumentCommandUseCase`에 바로 위임합니다.
조율할 것이 없기 때문이며, 규칙이 생기면 그때 서비스를 만듭니다.

---

## 엔드포인트

### `/api/admin/members` — `MemberController`

`POST /` 등록 · `GET /` 목록 · `GET /{id}` 단건 · `PUT /{id}` 수정 · `DELETE /{id}` 삭제

- **전 경로가 `@AuthenticationPrincipal`로 tenantId를 받습니다.** 단건 경로 3개(`GET`·`PUT`·`DELETE`)는
  2026-08-25에 추가된 것으로, 그전에는 다른 테넌트의 계정을 조회·수정·삭제할 수 있었습니다.
  **이 파라미터를 지우지 마세요.**
- 본문 없이 `ApiResponse.success()`만 반환하는 경로(`POST`·`PUT`)는 선언 타입도 `ApiResponse<Void>`입니다.
  `MemberResponse`로 선언하면 Swagger가 실제와 다른 스키마를 광고합니다.

### `/api/admin/documents` — `DocumentManagementController`

문서 등록·수정·버전 추가·삭제. 조회·다운로드는 `storage`의 `/api/documents`가 담당합니다
(읽기는 인증된 모든 사용자에게 열려 있고, 쓰기만 ADMIN 전용이라 경로가 갈립니다).

> **클래스명이 `DocumentController`가 아닌 이유**: `storage`에 같은 이름의 컨트롤러가 있어
> 스프링 빈 이름(`documentController`)이 충돌합니다. 규칙 7의 `{도메인}Controller`에서 벗어나지만
> 빈 충돌을 피하려면 어느 한쪽이 접미사를 가져야 합니다.

---

## 모듈 규칙

### tenant 소유권 격리

루트 `CLAUDE.md` 규칙 13을 따릅니다. 이 모듈은 원장이 없으므로 **격리는 전적으로 포트 호출에 달려 있습니다.**

- 컨트롤러가 `principal.getTenantId()`를 받아 Command에 실어 보내고, 원장 모듈이 그 범위로 조회합니다.
- **tenantId를 Command에 넣지 않고 빠뜨리면 그대로 교차 테넌트가 됩니다.** 이 모듈에는 그것을
  잡아 줄 자체 WHERE 절이 없습니다.
- `UpdateMemberCommand`에 `tenantId` 필드가 있는 이유가 이것입니다.

### 계층

`presentation` → `application` → (타 모듈 `port/in`) 구조입니다.
`domain/Member`는 있지만 규칙을 갖지 않는 표현 모델이고, `infrastructure`는 없습니다.
**Spring Data Repository나 타 모듈의 엔티티를 직접 참조하지 않습니다.**

---

## 향후 과제

- 이 모듈에는 테스트가 없습니다. 특히 **단건 경로의 tenant 격리**는 회귀가 곧 취약점이므로
  `UserQueryUseCase`를 Fake로 대체한 서비스 테스트가 필요합니다(루트 규칙 14).
- 문서 관리에 admin 고유 규칙이 생기면 `DocumentManagementController`의 위임을 서비스로 승격합니다.
