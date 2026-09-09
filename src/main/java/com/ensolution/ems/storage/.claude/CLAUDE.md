# storage 모듈 가이드라인

문서와 그 버전 이력을 관리하는 모듈입니다. 메타는 MySQL에, 파일 실물은 보관소(로컬 디스크 또는 S3)에 둡니다.

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

**이 모듈의 소유가 아닙니다.** 파일 실물 보관은 도메인 기능이 아니라 공통 인프라라서 `global/storage/`에 있고,
이 모듈은 그 SPI의 **소비자 중 하나**입니다(다른 하나는 `chat`의 대화 첨부입니다).

**환경마다 보관소는 하나입니다.** `ems.storage.provider`가 어느 구현체를 빈으로 올릴지 정하고,
서비스는 어느 보관소인지 모른 채 `storageKey`만 넘깁니다.

```
DocumentService        ChatMessageService
      │  (storageKey)        │
      ▼                      ▼
        FileStorageClient          ← global/storage/  (SPI)
              ▲
              ├── LocalFileStorageAdapter   (provider=LOCAL 또는 미설정)
              └── S3FileStorageAdapter      (provider=S3, S3Config가 배선)
```

- `global/storage/FileStorageClient` — 서비스가 보는 유일한 계약입니다. `store`·`load`·`delete` 셋뿐입니다.
  **감싸는 포트를 이 모듈에 따로 두지 않습니다**(루트 규칙 3·10의 단순 위임 래퍼 금지). 서비스가 직접 주입받습니다.
- 구현체·설정·빈 배선(`LocalFileStorageAdapter`·`S3FileStorageAdapter`·`StorageProperties`·`S3Config`)도
  전부 `global/storage/`에 나란히 있습니다. 둘 중 하나만 등록되므로 라우팅 계층이 없습니다.
- `storageKey`는 **소비 모듈의 도메인**이 만듭니다. 이 모듈에서는 `DocumentVersion`입니다.
  어댑터는 그 키를 받아 자기 방식으로 해석할 뿐이며, S3에서는 `keyPrefix`를 앞에 붙인 것이 오브젝트 키가 됩니다.
- **활성 보관소 값 자체는 `StorageProperties`에 바인딩하지 않습니다** — `@ConditionalOnProperty`가
  Environment에서 직접 읽어 빈 등록 시점에 판단하므로, record에는 선택된 보관소가 실제로 쓰는 설정만 남습니다.
- 실물이 없을 때 어댑터가 던지는 코드는 `STORAGE_FILE_NOT_FOUND`입니다. 어댑터가 공용이 되면서
  `DOCUMENT_FILE_NOT_FOUND`에서 이름을 바꿨습니다 — 문서만의 오류가 아니기 때문입니다.

### 보관소 선택

`ems.storage.provider`(환경변수 `STORAGE_PROVIDER`, 기본 `LOCAL`) 하나로 결정됩니다.

| 값 | 등록되는 빈 |
|---|---|
| `LOCAL` 또는 미설정 | `LocalFileStorageAdapter` |
| `S3` | `S3FileStorageAdapter` (+ `S3Client`) |
| 그 외 | 없음 → **기동 실패** |

- **오설정은 기동 시점에 터집니다.** 값이 둘 중 어느 것도 아니면 `FileStorageClient` 빈이 없어
  `DocumentService` 주입이 실패하고, `provider=S3`인데 `S3_BUCKET`이 비면 `S3Config.s3Client`가
  `IllegalStateException`을 던집니다. 첫 업로드에서 500이 나는 것보다 부팅 실패가 낫습니다.
- 자격증명은 설정에 두지 않습니다. AWS SDK 기본 체인이 IAM Role(EC2 인스턴스 프로파일·
  ECS Task Role·EKS IRSA)을 찾습니다.

> **보관소를 바꾸면 이전 보관소의 파일은 읽지 못합니다.** 한때는 버전 레코드마다 `provider`를 기록하고
> 읽기·삭제를 그 값으로 라우팅했지만(`RoutingFileStorageAdapter` + `FileStore` SPI + `StorageProvider` enum),
> **개발 단계라 과거 로컬 파일을 운영에서 읽을 일이 없다**는 전제로 전부 걷어냈습니다.
> 운영 데이터가 쌓인 뒤 보관소를 옮기게 되면 파일을 먼저 이관하고 전환해야 합니다
> (컬럼 제거 스크립트: `docs/migration/2026-09-06-storage-drop-provider.sql`).

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

**쓰기만 공개 계약입니다.** `admin`의 `/api/admin/documents`가 쓰기 유스케이스를 전부 위임하므로
그 경로의 타입만 `port/in`에 있고, 조회 경로는 이 모듈 안에서 끝나므로 `command/`에 있습니다.

| 위치 | 타입 | 근거 |
|---|---|---|
| `application/port/in/` | `DocumentCommandUseCase`, `CreateDocumentCommand`, `UpdateDocumentCommand`, `AddDocumentVersionCommand`, `UploadedFile` | `admin`이 소비하는 공개 계약 |
| `application/command/` | `DocumentSummary`, `DocumentVersionSummary`, `DocumentFile` | 조회 경로는 이 모듈 안에서 끝납니다 |

- **조회용 `DocumentQueryUseCase`는 두지 않습니다.** 타 모듈 소비자가 없어 공개 계약이 아니며,
  인터페이스를 두면 그 반환 VO까지 `port/in`으로 끌려 올라갑니다. `DocumentController`가
  `DocumentService`를 직접 주입합니다(다수 모듈의 기존 방식).
- `port/out`의 `DocumentRepository`·`DocumentVersionRepository`는 `command/`의 `~Summary`를 반환합니다.
  한때 `port/in`을 반환해 **인프라 계약과 공개 계약이 묶여 있었습니다.**

> **접미사 체계 예외 2건** — `UploadedFile`은 Command의 컴포넌트로 전이 노출되는 **입력** payload라
> `~Summary`가 아니고, `DocumentFile`은 바이트+파일명 다운로드 payload라 기존 접미사 어디에도 맞지
> 않습니다. 루트 `CLAUDE.md`의 접미사 표에 예외로 명시돼 있습니다.

### 매퍼

- `infrastructure/mapper/{DocumentEntityMapper, DocumentVersionEntityMapper}` — 엔티티 ↔ 도메인,
  그리고 목록 조회용 `~Summary` 생산
- `presentation/mapper/StorageDocumentMapper` — `admin`의 `AdminDocumentMapper`와 빈 이름이 겹치지 않도록
  모듈명을 접두로 붙였습니다

---

## 향후 과제

- `DocumentVersionRepository`에 tenantId를 넣어 **포트 자체로 격리가 드러나게** 하는 편이 안전합니다.
  현재는 호출 순서라는 관례에 의존합니다
- 파일 크기·확장자 제한, 바이러스 검사 등 업로드 정책
- 업로드·다운로드가 전 구간 온메모리 `byte[]`입니다. multipart 제한이 20MB라 아직 감당되지만,
  더 큰 파일을 다루게 되면 스트리밍이나 presigned URL을 검토해야 합니다
- `docs/DATABASE.md`에 `documents`·`document_versions` 섹션이 없습니다
- 이 모듈의 테스트는 `DocumentServiceTest`입니다. 보관소 어댑터 테스트(`S3FileStorageAdapterTest`)는
  어댑터와 함께 `src/test/.../global/storage/`로 옮겨갔습니다
- 버전 번호 부여와 "마지막 버전은 못 지운다" 규칙은 아직 회귀로 고정돼 있지 않습니다
