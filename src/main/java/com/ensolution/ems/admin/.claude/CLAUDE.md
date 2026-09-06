# admin 모듈 가이드라인

테넌트 **관리자(ADMIN)** 전용 관리 화면의 백엔드입니다. 회원 관리와 문서 관리 두 가지를 담습니다.
`/api/admin/**`는 `SecurityConfig`에서 `hasRole("ADMIN")`으로 보호됩니다.

---

## 자체 원장이 없는 모듈

**이 모듈은 자기 테이블을 갖지 않습니다.** `infrastructure/`도 `application/`도 없습니다.
`presentation/`만 있으며, 데이터는 전부 다른 모듈의 `port/in`에서 옵니다.

| 관심사 | 원장 소유 모듈 | 이 모듈이 쓰는 계약 |
|---|---|---|
| 회원 | `auth` | `UserQueryUseCase`, `UserCommandUseCase` (+ `UserSummary`·`Create/UpdateUserCommand`) |
| 문서 | `storage` | `DocumentCommandUseCase` (+ `Create/Update/AddDocumentVersionCommand`·`UploadedFile`) |

규칙은 원장 모듈이 소유합니다(예: 역할 부여 제한은 `auth`의 `UserValidator`).

> **원장을 갖지 않는 모듈은 규칙을 중복 구현하지 않습니다.** 검증을 여기 옮겨 오면 원장 모듈의
> 다른 호출 경로가 그 검증을 우회하게 됩니다.

### 중간 계층을 두지 않습니다

**컨트롤러가 타 모듈의 `port/in`에 직접 위임하고, presentation 매퍼가 Request를 그 모듈의 Command로 곧장 바꿉니다.**

```
MemberController → MemberMapper → auth의 Create/UpdateUserCommand → User*UseCase
                                → auth의 UserSummary → MemberResponse

DocumentManagementController → AdminDocumentMapper → storage의 Command → DocumentCommandUseCase
```

한때 회원 쪽에는 `domain/Member` + `Create/UpdateMemberCommand` + `MemberService` + `MemberPortMapper`가
있었지만 **2026-09-06에 걷어냈습니다.** 이유:

- `Member`는 `UserSummary`와 필드가 같고(`id` ↔ `userId`만 다름) 행위가 없어 도메인 모델이 아니었습니다.
  `Create/UpdateMemberCommand`도 auth의 것과 사실상 동일했습니다.
- `MemberService`는 5개 메서드가 전부 단순 위임이었습니다 — 조율할 것이 없었습니다.
- "auth가 `UserSummary`를 바꿔도 매퍼 하나만 고치면 된다"는 이점이 실제로는 없었습니다.
  MapStruct 자동 매핑이라 **필드가 늘면 `Member`에도 같은 이름으로 더해야** 값이 넘어왔고,
  `unmappedTargetPolicy`가 기본값이라 누락이 조용히 지나갔습니다.
- 같은 모듈의 문서 관리는 처음부터 중간 계층 없이 동작하고 있었습니다. 두 리소스가 서로 다른 구조일 이유가 없습니다.

근거는 루트 `CLAUDE.md`의 **공유 커널** 예외입니다 — 포트 시그니처에 이미 드러난 타입을 다시 감싸면
변환 계층만 늘고 얻는 것이 없습니다.

> **되살릴 때**: 회원 상태(활성·정지)·초대·최근 로그인처럼 **admin 고유 개념**이 생기면 그때
> `application/`을 만들고 서비스로 승격합니다. auth의 `User`에 넣을 수 없는 필드가 생겼다는 것이 신호입니다.
> 지금 없는 것을 대비해 두지 않습니다.

### 응답 계약은 `MemberResponse`가 지킵니다

중간 도메인이 없어도 대외 언어는 "회원"으로 유지됩니다. `MemberMapper`는 `unmappedTargetPolicy = ERROR`라
auth가 `UserSummary`에 필드를 더하면 **컴파일이 깨져** 응답 계약을 함께 검토하게 됩니다.

---

## 엔드포인트

### `/api/admin/members` — `MemberController`

`POST /` 등록 · `GET /` 목록 · `GET /{id}` 단건 · `PUT /{id}` 수정 · `DELETE /{id}` 삭제

- **전 경로가 `@AuthenticationPrincipal`로 tenantId를 받아 Command에 싣습니다.** 단건 경로 3개
  (`GET`·`PUT`·`DELETE`)는 2026-08-25에 추가된 것으로, 그전에는 다른 테넌트의 계정을 조회·수정·삭제할
  수 있었습니다. **이 파라미터를 지우지 마세요.** 회귀는 `MemberControllerTest`가 잡습니다.
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
- **tenantId를 Command에 넣지 않아도 컴파일은 통과합니다.** 그 자리에서 교차 테넌트가 되며,
  이 모듈에는 그것을 잡아 줄 자체 WHERE 절이 없습니다.
- `MemberControllerTest`가 전 경로에서 tenantId가 실리는지 고정합니다.

### 계층

`presentation` → (타 모듈 `port/in`) 구조입니다. `application`도 `domain`도 `infrastructure`도 없습니다.
**Spring Data Repository나 타 모듈의 엔티티를 직접 참조하지 않습니다** — 참조하는 것은 `port/in`뿐입니다.

---

## 향후 과제

- 문서 관리 경로에는 테스트가 없습니다. 회원 쪽과 같은 방식으로 tenantId·uploadedBy 전달을 고정하면 됩니다
- 문서 관리에 admin 고유 규칙이 생기면 `DocumentManagementController`의 위임을 서비스로 승격합니다
