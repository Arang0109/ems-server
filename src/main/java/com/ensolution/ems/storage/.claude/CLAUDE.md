# storage 모듈 가이드라인

문서와 그 버전 이력을 관리하는 모듈입니다. 메타는 MySQL에, 파일 실물은 보관소(현재 로컬 디스크)에 둡니다.

---

## 애그리거트

| 애그리거트 | 저장소 | 도메인 루트 | 비고 |
|---|---|---|---|
| **Document** | MySQL `documents` | `domain/Document` | tenant 종속. `latestVersionNo`를 보유 |
| **DocumentVersion** | MySQL `document_versions` | `domain/DocumentVersion` | 문서당 N건. 실물 파일의 `storageKey` 보유 |

```
Document (문서)
  └── DocumentVersion (버전)  1:N   ── storageKey ──> 파일 실물 (FileStorageClient)
```

버전은 **덮어쓰지 않고 쌓입니다.** 양식 문서가 개정돼도 과거 버전을 받아야 하는 실무 요구가 있기 때문입니다.

---

## 트랜잭션과 파일 쓰기의 순서

**파일 쓰기는 트랜잭션과 함께 롤백되지 않습니다.** 그래서 순서를 고정합니다.

- **메타를 먼저 저장하고 파일 쓰기를 마지막에** 둡니다(`DocumentService.storeVersion`).
  파일 쓰기가 실패하면 트랜잭션이 롤백되어 **실물 없는 레코드가 남지 않습니다.**
- 반대 순서였다면 메타 저장 실패 시 주인 없는 파일이 디스크에 남습니다.
- 삭제는 반대입니다 — 메타를 지운 뒤 파일을 지웁니다. 파일 삭제가 실패해도 메타는 이미 없으므로
  고아 파일만 남고, 이는 레코드가 가리키는 파일이 없는 것보다 덜 위험합니다.
  (`FileStorageClient.delete`는 대상이 이미 없어도 예외를 던지지 않습니다.)

> 같은 성격의 규약이 `schedule`(MySQL↔MongoDB)에도 있습니다. 2PC를 걸 수 없는 두 저장소를
> 다룰 때의 공통 원칙 — **되돌릴 수 없는 쪽을 마지막에** 둡니다.

---

## 유스케이스 (`DocumentService`)

| 메서드 | 규칙 |
|---|---|
| `createDocument` | 문서명 tenant 내 유일(`DocumentValidator.requireUniqueName`). 생성과 동시에 1번 버전을 만듭니다 |
| `addVersion` | 다음 버전 번호를 부여하고 문서의 `latestVersionNo`를 올립니다 |
| `updateDocument` | 메타만 수정. 파일은 건드리지 않습니다 |
| `deleteVersion` | **마지막 한 개는 남깁니다** — 버전이 0개면 문서가 다운로드 불가 상태로 남습니다(`DOCUMENT_LAST_VERSION_NOT_DELETABLE`). 최신 버전을 지운 경우에만 `latestVersionNo`를 남은 최대값으로 내립니다 |
| `deleteDocument` | 버전 전체 + 파일 전체를 함께 정리 |
| `download` | `versionNo`가 null이면 최신 버전 |

---

## 보관소 추상화

`application/port/out/FileStorageClient` — 파일 실물의 보관소입니다.
구현체를 교체해 저장 위치를 바꿉니다. 현재는 `LocalFileStorageAdapter` 하나뿐입니다.

- `domain/StorageProvider`(`S3`, `LOCAL`) — **버전 레코드에 어디에 저장했는지 기록**합니다.
  보관소를 바꿔도 과거 파일을 어디서 찾아야 하는지 알 수 있어야 하기 때문입니다.
- `storageKey`는 도메인이 만듭니다. 어댑터는 그 키를 받아 자기 방식으로 해석할 뿐입니다.
- 설정은 `infrastructure/config/StorageProperties`입니다.

---

## 엔드포인트

### `/api/documents` — `DocumentController` (조회·다운로드)

`GET /` 목록(카테고리 필터) · `GET /{id}` 단건 · `GET /{id}/versions` 버전 목록 ·
`GET /{id}/download` 최신 다운로드 · `GET /{id}/versions/{versionNo}/download` 특정 버전 다운로드

**읽기는 인증된 모든 사용자에게 열려 있습니다.** 성적서·채취기록부 양식처럼 실무자가 직접 받아야 하는
문서가 있기 때문입니다. 조회 범위는 항상 요청자의 tenant로 제한됩니다.

**쓰기(등록·수정·버전 추가·삭제)는 `admin` 모듈의 `/api/admin/documents`가 담당합니다** — ADMIN 전용입니다.
권한 층위가 달라 경로를 나눴습니다.

> **다운로드 2개는 `ApiResponse`로 감싸지 않습니다**(`ResponseEntity<byte[]>`).
> 바이너리를 그대로 내려보내야 하기 때문이며, 루트 `CLAUDE.md` 규칙 6에 예외로 명시돼 있습니다.
> `Content-Disposition` 파일명은 한글이 깨지지 않도록 URL 인코딩합니다.

---

## 모듈 규칙

### tenant 소유권 격리

루트 `CLAUDE.md` 규칙 13을 따릅니다. 이 모듈에서 주의할 점:

- `DocumentRepository`는 전 메서드가 `(id, tenantId)`를 받습니다.
- **`DocumentVersionRepository`는 tenantId를 받지 않습니다.** `findByDocumentIdAndVersionNo`,
  `deleteAllByDocumentId` 등이 `documentId`만 받습니다.
  - **부모 경유 격리**입니다 — 모든 호출 경로가 `documentRepository.findById(documentId, tenantId)`로
    문서 소유권을 **먼저** 확인한 뒤 버전을 다룹니다.
  - **포트 시그니처만 보면 격리가 보이지 않습니다.** 버전 포트를 문서 확인 없이 호출하는 코드를
    새로 만들면 그 자리에서 교차 테넌트가 됩니다. `DocumentService`의 기존 메서드들이
    하나같이 `findById`로 시작하는 이유입니다.

### command·VO 위치

이 모듈은 `application/command/`가 없고 Command·VO를 전부 `application/port/in/`에 둡니다
(`CreateDocumentCommand`·`UpdateDocumentCommand`·`AddDocumentVersionCommand`·`DocumentFile`·`UploadedFile`·
`DocumentSummary`·`DocumentVersionSummary`).

`admin`이 쓰기 유스케이스를 전부 위임하므로 **대부분이 실제로 공개 계약**이라 이 배치가 성립합니다.
다만 `DocumentFile`·`UploadedFile`은 `~Summary`가 아니어서 접미사 체계에서 벗어나 있습니다.
모듈 내부 전용 타입이 생기면 그때는 `application/command/`를 만들어 나눕니다.

### 매퍼

- `infrastructure/mapper/{DocumentEntityMapper, DocumentVersionEntityMapper}` — 엔티티 ↔ 도메인,
  그리고 목록 조회용 `~Summary` 생산
- `presentation/mapper/StorageDocumentMapper` — `admin`의 `AdminDocumentMapper`와 빈 이름이 겹치지 않도록
  모듈명을 접두로 붙였습니다

---

## 향후 과제

- `DocumentVersionRepository`에 tenantId를 넣어 **포트 자체로 격리가 드러나게** 하는 편이 안전합니다.
  현재는 호출 순서라는 관례에 의존합니다
- `LocalFileStorageAdapter` 외 S3 어댑터 (`StorageProvider.S3`는 이미 정의돼 있음)
- 파일 크기·확장자 제한, 바이러스 검사 등 업로드 정책
- `docs/DATABASE.md`에 `documents`·`document_versions` 섹션이 없습니다
- 이 모듈에는 테스트가 없습니다. 버전 번호 부여와 "마지막 버전은 못 지운다" 규칙이 우선 대상입니다
