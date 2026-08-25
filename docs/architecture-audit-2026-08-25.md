# 아키텍처 규칙 준수 진단 리포트 (2026-08-25)

**점검일** 2026-08-25
**범위** `src/main/java/com/ensolution/ems/**` 558파일 + `src/test/**` 41파일, 루트 `CLAUDE.md`·`ARCHITECTURE.md`, 모듈별 `.claude/CLAUDE.md`, `docs/**`, `build.gradle`
**선행 리포트** [`architecture-audit-2026-08-24.md`](architecture-audit-2026-08-24.md) — 어제 진단 + 치명 3건 조치 내역
**성격** 재검증 + **이번 회차에 실제로 조치한 내역**을 함께 기록합니다.

> 어제 리포트는 이력으로 보존합니다. 이 문서는 그 이후 상태를 기준으로 합니다.

---

## 0. 요약

| 구분 | 건수 |
|---|---|
| 이번 회차 조치 — 문서 | 루트 2 + 모듈 정정 4 + 모듈 신규 4 + `.claude` 자산 3 |
| 이번 회차 조치 — 코드 | 6건 (파일 이동 6 · 어노테이션 1 · 반환 타입 2 · 빈 디렉터리 5) |
| 규칙 확장으로 해소 (코드 무변경) | 5개 범주, 이탈 약 40건 |
| **이월** | 9건 |
| 신규 발견 | 4건 |

**한 줄 결론**: 어제 리포트가 지적한 항목 중 **규칙이 좁아서 생긴 "형식상 위반"은 규칙 확장으로 정리했고,
파일 이동으로 끝나는 명문 위반은 전부 조치했습니다.** 남은 것은 구조를 건드려야 하는 9건이며,
그중 **H1(global의 모듈 경계 관통)과 M3(`domain/port/` 잔존)이 우선순위 상위**입니다.

문서 측면에서 가장 큰 변화는 **tenant 소유권 격리 규칙을 루트로 승격**한 것과
**문서가 없던 4개 모듈(auth·admin·contract·storage)에 문서를 만든 것**입니다.
어제 리포트가 "모듈 문서 부재가 곧 보안 결함으로 이어졌다"고 지적한 바로 그 모듈들입니다.

검증: 컴파일 통과, **전체 테스트 367개 전부 통과**(실패 0).

---

## 1. 이번 회차 조치 — 코드

전부 파일 이동·시그니처 수준이며 동작 변경은 없습니다.

| # | 조치 | 영향 | 해소한 규칙 위반 |
|---|---|---|---|
| 1 | **`platform/application/result/` 해체** — `TenantSummary` → `application/port/in/`, `TenantListItem` → `application/command/` | 2파일 + import 10곳 | 규칙 8 명문 위반(`application/result/` 금지) + **모듈 경계 위반**(`schedule`이 `platform`의 `port/in`이 아니라 `result/`를 직접 참조) + `platform/.claude/CLAUDE.md` 문서 불일치 — **셋 동시 해소** |
| 2 | `auth/application/port/RefreshTokenStore` → `application/port/out/` | 참조 2곳 | 규칙 4 포트 위치 (`port/` 직하 금지) |
| 3 | `contract/application/port/ContractQueryUseCase` → `application/port/in/` | 참조 2곳 | 규칙 4. 한 모듈에서 UseCase 위치가 갈리던 문제 |
| 4 | `global/common/enums/{SubscriptionPlan, TenantStatus}` → `platform/domain/` | import 6곳(전부 platform 내부) | 규칙 2 (단일 모듈 전용 enum) |
| 5 | `auth/domain/port/UserRepository` — `@Repository` + import 제거 | 1파일 | 규칙 1. **프로젝트 전체에서 유일했던 domain 계층의 spring import** |
| 6 | `admin/MemberController` — `createMember`·`updateMember` 반환 타입을 `ApiResponse<Void>`로 | 1파일 | 규칙 6. 선언은 `MemberResponse`인데 본문은 `ApiResponse.success()`라 Swagger 스키마가 실제와 불일치 |
| 7 | 빈 디렉터리 5개 제거 | — | 잔재 (`contract/.claude`는 신규 문서로 채움) |

`application/result/` 패키지는 이제 프로젝트에 존재하지 않습니다.
`application/port/` 직하 파일도 0건입니다.

---

## 2. 이번 회차 조치 — 규칙 확장 (코드 무변경)

`schedule` 모듈이 도입한 패턴들은 **코드가 옳고 규칙 표가 좁았던** 경우입니다.
루트 `CLAUDE.md`를 넓혀 형식상 위반을 없앴습니다.

| 확장 | 내용 | 해소된 이탈 |
|---|---|---|
| **접미사 2종 추가** | `~ExportView`(외부 템플릿 엔진 바인딩, **record 예외**), `~Event`(알림 페이로드) | 17건 (`ExportView` 15 + `SheetsSavedEvent`·`EditorRef`) |
| **Application 협력자 5종** | `{대상}Assembler`·`Recorder`·`Finder`·`Indexer`·`Recalculator`. 전부 `@Component` + `@Transactional` 미부착 | 14건 |
| **매퍼 4번째 범주** | "포트 계약 생산 매퍼"(`{대상}SummaryMapper`) — 자기 도메인 → 자기 `port/in` 공개 계약 | 3건 (`ContractSummaryMapper`·`InspectionDueSummaryMapper`·`TenantSummaryMapper`) |
| **매퍼 메서드명 일반화** | `to{동작}Command()` 형태를 명시적으로 허용. `toDocument()`(Mongo) 추가. MapStruct 예외 조항 | 40여 종 |
| **`ApiResponse` 예외 명문화** | 바이너리 `ResponseEntity<byte[]>`·SSE `SseEmitter` 5곳 | 5건 |
| **공유 커널 조항** | 규칙 1 — 공급 모듈이 `port/in`에 노출한 도메인 타입은 소비 모듈이 직접 참조 가능. 규칙 2 — `global/common/enums/`는 둘 이상이 공유하는 enum만 | 12곳 import + enum 8개 |

### 루트로 승격한 규칙 3건

| 규칙 | 원래 위치 | 승격 이유 |
|---|---|---|
| **§13 tenant 소유권 격리** | `client_management/.claude/CLAUDE.md`에만 | **최우선.** 이 규칙이 모듈 문서에만 있어 문서 없는 `auth`·`admin`·`contract`가 사정권 밖이었고, 어제의 치명 3건이 정확히 그 세 모듈에서 나왔습니다. 역할 부여 제한도 함께 포함 |
| **§14 테스트 규칙** | 문서화 전무 (코드에만) | Fake 리포지토리 + 생성자 직접 조립 + AssertJ가 사실상 표준인데, `.claude/agents/test-writer.md`는 "Mockito/@SpringBootTest"를 지시하고 있었습니다 |
| **§15 스키마 마이그레이션 정책** | `docs/migration/` 주석과 `equipment` 문서에 분산 | Flyway 부재 + `ddl-auto: update` = 삭제가 자동 반영되지 않음. 배포 절차와 직결 |

---

## 3. 이번 회차 조치 — 문서

### 루트
- **`CLAUDE.md`** — 기술 스택에 MongoDB·jxls·테스트 추가(폴리글랏 명시), 모듈 지도 10개, 규칙 확장(§2), 신규 규칙 3건, 저장소 문서 지도, 빌드·실행(WSL 주의), 커밋 컨벤션. `tenant` 모듈 오기 2곳 정정
- **`ARCHITECTURE.md`** — 모듈 목록 4→10, **"Tenant 애그리거트의 소유권" 절 전면 재작성**(`tenant` 모듈 전제 제거), 패키지 템플릿에 실재 서브패키지 반영, **폴리글랏 저장소 절 신설**, `tenant_id` 두 갈래 각주

### 모듈 문서 정정
| 파일 | 정정 |
|---|---|
| `platform/.claude/CLAUDE.md` | `tenant` 모듈 참조 3곳 제거. "TenantEntity를 소유하지 않는다"는 **사실과 반대**였으므로 소유 관계로 재작성 |
| `client_management/.claude/CLAUDE.md` | Validator 표 렌더링 버그(`requireMemberInTenant`가 `PreventionValidator` 행에 붙어 있었음), Assembler 위치를 `service/assembler/`로, `StackSnapshotAssembler` 추가, `port/in` 목록 확장(3 UseCase + VO 5), tenant 격리 절은 루트 참조로 축약 |
| `equipment/.claude/CLAUDE.md` | 공유 커널 공개 범위를 **표로 명시** — sealed 하위 구체 타입까지 포함. Mongo `_class` 판별자 경고 추가 |
| `schedule/.claude/CLAUDE.md` | "3단 낙관적 락" → **2단**(표에 행이 2개뿐이었음). 층을 나누는 이유 보강 |

### 모듈 문서 신규 (4개)
`auth` · `admin` · `contract` · `storage` — 어제 리포트 G2가 지적한 공백입니다.
각 모듈이 안고 있는 판단 항목에 근거를 부여하는 데 초점을 뒀습니다.

### `.claude/` 자산
- `commands/마무리.md` — **낡은 규칙으로 검사하고 있었습니다.** [B]가 `domain/port` 경유를 요구하고(현 표준은 `application/port/out/`), [D]가 `Jpa{도메인}Entity`를 요구(현 규칙은 `{도메인}Entity`). 현행화하고 **[F] tenant 격리 체크리스트를 신설**(위반 시 종합 판정 ❌)
- `agents/test-writer.md` — 실제 스타일과 충돌하던 지시를 규칙 14에 맞게 전면 재작성
- 두 커맨드의 `Co-Authored-By` 모델명 하드코딩 제거

---

## 4. 이월 — 남은 항목

우선순위 순입니다. 전부 **구조를 건드려야 하므로** 이번 범위에서 제외했습니다.

### 4-1. `global`이 두 모듈의 JPA 계층을 직접 관통 (H1)

`global/security/user/CustomUserDetailsService.java:3-5, 8-9, 26-27`

```
auth.infrastructure.entity.{UserEntity, RoleEntity}
auth.infrastructure.repository.UserJpaRepository
platform.infrastructure.entity.TenantEntity
platform.infrastructure.repository.TenantJpaRepository
```

**프로젝트에서 유일하게 뚫린 모듈 경계입니다.** application·presentation 계층의 infrastructure 참조는
전수 검색 결과 0건인데 `global`만 예외입니다.

- 수정 방향: `auth`의 `UserQueryUseCase`, `platform`의 `TenantQueryUseCase` 경유
- **주의**: `UserDetailsService`는 Spring Security 초기화 시점에 관여하므로 순환 참조 확인이 필요합니다.
  현재 `UserSummary`에 비밀번호가 없어 인증에 쓰려면 포트 확장도 필요합니다

### 4-2. `domain/port/` 잔존 이관 (M3 + H3)

`auth` 6개(`Authenticator`·`PasswordEncryptor`·`RoleRepository`·`TokenIssuer`·`TokenParser`·`UserRepository`),
`contract` 1개(`ContractRepository`).

`contract/domain/port/ContractRepository.java:3`이 `application/command/ContractListItem`을 import 해
**`domain → application` 역방향 의존**이 됩니다(H3). 포트를 `application/port/out/`으로 옮기면 함께 사라집니다.

> `auth`는 규칙 4에서 **위치**에 대해서만 유예를 받았을 뿐, 이관 시 `Optional` 반환 3건도 함께 검토 대상입니다.
> 다만 이 3건은 "리소스 존재 은닉"이라는 의도가 javadoc에 명시돼 있어, 규칙에 예외를 두는 편이 나을 수 있습니다.

### 4-3. 어셈블러 위치 통일

`application/service/assembler/`가 표준(문서 반영 완료)이나 실제로는 8개가 `service/` 직하에 있습니다.
`client_management/TeamAssembler`, `contract/ContractDetailAssembler`, `schedule` 6개.

### 4-4. `schedule/application/command/event/` → `application/event/`

`SheetsSavedEvent`·`EditorRef` 2파일 + 참조 5곳. 다른 모듈은 이미 `application/event/`를 씁니다.
`EditorRef`는 접미사가 없으므로 함께 정리하는 편이 좋습니다.

### 4-5. presentation 구조 (M4·M5)

- `contract/presentation/ContractController` — **프로젝트 유일**하게 `controller/` 서브디렉터리가 없음
- `schedule/presentation/` — `analysis/`·`history/`는 애그리거트별, Schedule 본체는 flat.
  `domain/`은 이미 4개로 나뉘어 있어 계층 간 비대칭 (schedule 문서의 자체 향후 과제)

### 4-6. `ScheduleService` 632줄 분해

유스케이스 14개 + analysis·이력 동기화 훅. 분해 축 후보는 schedule 문서에 정리돼 있습니다.

### 4-7. 응답 DTO의 UI 용어 (M12)

- `dashboard/.../MeasurementCountChartResponse` — `Chart`. 소스 VO가 `MeasurementCountItem`이므로
  `MonthlyMeasurementCountResponse`가 적절
- `schedule/.../FulfillmentBoardResponse` — `Board`, 그리고 중첩 record가 `Row`/`Cell`이라
  **표 좌표계를 응답 계약에 그대로 노출**. 매퍼도 `toRowResponse`/`toCellResponse`

### 4-8. ErrorCode 정리

- `schedule/domain/sheet/SheetMerge.java:94` — 고정 문구 인라인 override.
  `SCHEDULE_SHEET_CATEGORY_REQUIRED` 신설이 정석 (`ScheduleService:333`은 동적 값이라 정당)
- `INVALID_INPUT` — `BAD_REQUEST`와 의미 중복
- `VALUE_MUST_NOT_BE_NULL` — HTTP 상태 개념이 아닌 필드 수준 검증. 인라인 override의 원인

### 4-9. `build.gradle` — `mapstruct-processor` annotationProcessor 중복 선언 (44·47행)

---

## 5. 신규 발견

어제 리포트에 없던 항목입니다.

### N1. `auth` ↔ `client_management` 상호 참조 `[판단]`

| 방향 | 위치 |
|---|---|
| auth → client_management | `AuthService`가 로그인 응답의 소속 팀을 `TeamQueryUseCase`로 조회 |
| client_management → auth | `TeamAssembler`·`TeamValidator`가 사수·부사수 정보를 `UserQueryUseCase`로 조회 |

**두 모듈이 서로의 `port/in`을 참조하는 유일한 쌍입니다.** 양쪽 모두 공개 계약만 쓰므로 규칙 위반은
아니지만, 순환이 깊어지면 한쪽을 떼어내기 어려워집니다.

소속 팀을 인증 principal이나 토큰 클레임에 넣으면 `auth → client_management` 방향을 끊을 수 있습니다.
다만 팀 변경이 토큰 만료까지 반영되지 않는 문제가 생기므로 트레이드오프입니다.

### N2. `AuthService.register`가 `IllegalArgumentException`을 던짐 `[코드]`

```java
if (userRepository.existsByUsername(username)) {
    throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
}
```

규칙 12는 비즈니스 규칙 거부를 `CustomException` + `ErrorCode`로 다루도록 합니다.
`USER_USERNAME_DUPLICATED`(409) 신설이 적절합니다. 현재는 `GlobalExceptionHandler`의 4xx/5xx 로그 레벨
분기도 타지 못합니다.

### N3. `ContractQueryUseCase.deleteContracts` — 이름과 동작 불일치 `[판단]`

`Query`라는 이름의 UseCase가 삭제를 수행합니다. 사업장 삭제 이벤트의 캐스케이드 전용 계약이므로
`ContractLifecycleUseCase` 같은 이름이 정확합니다.

### N4. `contract` 통계가 전체 목록을 메모리에서 집계 `[코드]`

`ContractService.countContracts`·`countContractsInMonth`·`findExpiringBetween`이
`findAllByTenantId(tenantId)`로 **전체를 읽은 뒤** 자바에서 셉니다. 계약이 늘면 COUNT·범위 쿼리로
내려야 합니다. `dashboard`가 호출하는 경로라 대시보드 로딩에 직결됩니다.

---

## 6. 준수 확인 — 전수 검색 위반 0건

| 규칙 | 결과 |
|---|---|
| presentation → infrastructure | **0건** |
| application → infrastructure | **0건** (§4-1의 `global` 1건 제외) |
| domain → JPA(`jakarta.persistence`) / `jakarta.validation` | **0건** |
| domain → spring framework | **0건** (이번 회차에 마지막 1건 제거) |
| domain → application | **1건** (§4-2 `ContractRepository`뿐) |
| `@Autowired`/`@Inject`/`@Resource` 필드 주입 | **0건** |
| `application/result/` · `application/port/` 직하 파일 | **0건** (이번 회차에 해소) |
| 매퍼 위치 이탈 | **0건** — 54개 전부 세 위치 중 하나 |
| 컨트롤러 `@Tag` | **24/24** |
| Validator 규칙 (규칙 10) | **13개 전부 준수** — 위치·명명·`require*` 접두·`@Transactional` 미부착·port만 주입. 모듈 4개로 확산 |
| Command 프리픽스 `{동작}{대상}` | 전부 준수, 역순 0건 |
| 컨트롤러·서비스의 수동 매핑 | **0건** |
| 인라인 ErrorCode override | **2건** (558파일 중) |
| tenant 격리 — 단건 조회 포트 | **14개 중 14개**가 `(id, tenantId)`. 미적용 3개는 전부 의도된 전역 리소스 |

---

## 7. 문서·테스트 공백 (조치 대상 아님, 기록)

### 테스트

| 모듈 | 테스트 | 비고 |
|---|---|---|
| schedule | 28 | |
| client_management | 10 | |
| equipment | 2 | |
| **auth·admin·contract·dashboard·platform·storage·global** | **0** | |

**어제의 치명 3건이 정확히 테스트 0인 모듈에서 나왔습니다.** Repository Adapter·JPA 매핑·Security
통합 테스트는 전 모듈에 걸쳐 없습니다.

### `docs/DATABASE.md`

- **섹션 없는 테이블**: `teams`, `documents`, `document_versions`
- `contract_table_view`(`@Subselect` 뷰) 미언급
- `## contract` 제목이 실제 테이블명 `contracts`와 불일치 (다른 섹션은 전부 실제 테이블명)
- MongoDB 컬렉션의 **의도적 부재 사유**가 문서에 없어 누락으로 읽힘
- 자체 후속 과제 5건 중 **Auditing 리스너 누락**(clients·workplaces·facilities·preventions·teams)이 실질적

### 미커밋 문서

`schedule/.claude/CLAUDE.md`, `docs/excel-template-guide.md`, `architecture-audit-2026-08-24.md`,
`docs/migration/2026-08-25-schedule-drop-status-log.sql`이 전부 untracked입니다.
**유실 위험이 있고 다른 세션에서 git으로는 볼 수 없습니다.** 커밋을 권합니다.

---

## 8. 이 리포트가 다루지 않은 것

- **전면적인 보안 점검** — 어제 §1의 3건은 *테넌트 격리 규칙 준수 여부*를 확인하다 드러난 것이며,
  인증·인가 경로 전반(토큰 수명·회전, CORS, 쿠키 속성, 입력 검증, 파일 업로드)은 살펴보지 않았습니다.
  `/security-review` 등으로 별도 점검을 권합니다
- 성능·쿼리 최적화 (N+1, 인덱스) — §5 N4만 규칙 점검 중 드러난 것을 기록했습니다.
  `dashboard/.claude/CLAUDE.md`의 "향후 과제"에 별도 정리가 있습니다
- 비즈니스 로직 정확성 — 규칙 준수 여부만 봤습니다

### 점검 방법

- `src/main` 558개 + `src/test` 41개 파일 대상 전수 grep (FQCN·와일드카드 우회 포함)
- 탐색은 병렬 에이전트 3개(미해결 항목 재검증 · 구조/네이밍 전수 · 문서 드리프트)로 수행하고,
  **조치 대상과 신규 발견은 원본 파일을 직접 열어 재확인**했습니다
- 조치 후 컴파일 + 전체 테스트 367개 통과 확인
- 라인 번호는 2026-08-25 조치 **이후** 워킹트리 기준입니다
