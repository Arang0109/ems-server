# 서버 코드 개선 진단 리포트 (2026-09-06)

**점검일** 2026-09-06
**범위** `src/main/java/com/ensolution/ems/**` 561파일 + `src/test/**` 55파일(테스트 클래스 42), 루트 `CLAUDE.md`·`ARCHITECTURE.md`, 모듈별 `.claude/CLAUDE.md`, `docs/**`, `build.gradle`, 개발 DB 실데이터, 프론트엔드 `ems-web`의 역할 사용처
**선행 리포트** [`architecture-audit-2026-08-25.md`](architecture-audit-2026-08-25.md) — 이월 9건 + 신규 4건
**성격** 진단 + **같은 회차에 조치한 내역**을 함께 기록합니다. 아키텍처 규칙뿐 아니라 성능·테스트·운영까지 봅니다.

> 선행 두 리포트는 아키텍처 규칙 준수에 한정했습니다. 이번에는 **규칙은 지켰지만 문제가 되는 코드**까지 봅니다.

---

## 0. 요약

| 구분 | 건수 |
|---|---|
| **이번 회차 조치** | 10건 (storage 단순화 · 고아 파일 · H2 · H3 · 역할 목록 · Member 계층 · **H1** · M2 · M4 · L1·L2) |
| 테스트 | 393 → **452 케이스** (실패 0) |
| **남은 발견 — 높음** | **0건** |
| 남은 발견 — 중간 | 3건 |
| 남은 발견 — 낮음 | 3건 |
| 선행 리포트 이월 중 해소 | 8건 |

**한 줄 결론**: **높음 등급이 0건이 됐습니다.** 3회차 연속 이월이던 `CustomUserDetailsService`의 모듈 경계
관통(H1)을 닫았고, 그 과정에서 드러난 **빈 순환 두 곳**까지 원인을 제거했습니다(`@Lazy`로 덮지 않았습니다).

`domain/port/` 패키지는 사라졌고, 전 모듈의 Outbound Port가 `application/port/out/`으로 정렬됐습니다.
남은 것은 성능 항목(M1·M3)과 테스트가 아직 없는 두 모듈(M4)이며, 모두 데이터가 늘기 전에는 급하지 않습니다.

---

## 1. 이번 회차 조치

### 1-1. storage 보관소 추상화 단순화 (라우팅 계층 제거)

```
이전                                    이후
FileStorageClient                       FileStorageClient
      ▲                                       ▲
RoutingFileStorageAdapter                     ├── LocalFileStorageAdapter  (provider=LOCAL/미설정)
      ├──▶ LocalFileStore                     └── S3FileStorageAdapter     (provider=S3)
      └──▶ S3FileStore
```

라우팅이 존재한 이유는 "보관소를 바꾼 뒤에도 과거에 다른 보관소로 올린 파일을 읽는다"였습니다.
개발 단계라 그 전제가 성립하지 않아 **전제와 함께 구조를 걷어냈습니다.**

| 구분 | 내역 |
|---|---|
| 삭제 | `RoutingFileStorageAdapter`, `FileStore` SPI, `StorageProvider` enum, `StoredFileRef`, `ErrorCode.STORAGE_PROVIDER_UNAVAILABLE`, `RoutingFileStorageAdapterTest` |
| 이동 | `infrastructure/storage/{Local,S3}FileStore` → `infrastructure/adapter/{Local,S3}FileStorageAdapter` (본문 로직 유지) |
| 포트 축소 | `FileStorageClient` — `activeProvider()` 제거, `load`/`delete`의 provider 인자 제거 |
| 영속 | `document_versions.provider` 컬럼 제거 (`2026-09-06-storage-drop-provider.sql`, 멱등 · **개발 DB 적용 완료**) |

**계획에 없던 판단 1건** — `S3Config.s3Client`에 버킷 blank 검사를 추가했습니다. 조건축이
"버킷이 채워졌는가"에서 "provider=S3인가"로 바뀌면서 버킷 없이 S3로 뜨는 구멍이 생기기 때문입니다.

검증한 기동 분기: `S3`+버킷 → 정상 / `S3`+버킷 없음 → `IllegalStateException` / `FOO` → 빈 없음으로 기동 중단.
LOCAL 경로는 등록→다운로드→버전 추가→버전 삭제→문서 삭제까지 실 API로 왕복 확인했습니다.

### 1-2. 고아 파일 정리

DB `storage_key` 9건과 디스크 10건을 대조해 **고아 1건**(`1/3/3/83b04925….xlsx`, 1.1MB — 문서 3의
버전 3으로 레코드는 지워졌는데 실물만 남음)을 백업 후 제거했습니다. 빈 버전 디렉터리 11개도 함께 정리.
결과는 **디스크 9건 = DB 9건 완전 일치**, 역방향(레코드는 있는데 실물 없음)은 0건.

### 1-3. H2 — 중복 아이디가 500을 반환하던 버그

`AuthService.register`가 `IllegalArgumentException`을 던져 `@ExceptionHandler(Exception.class)`가
받아 **500 INTERNAL_SERVER_ERROR**로 나가고, 정상적인 비즈니스 거부가 `log.error`로 남아 알람 대상이
되던 문제입니다.

`ErrorCode.USER_USERNAME_DUPLICATED(CONFLICT, "이미 사용 중인 아이디입니다.")`를 신설해 교체했고,
실 API로 확인했습니다:

```
POST /api/admin/members (중복 아이디) → HTTP 409  {"message":"이미 사용 중인 아이디입니다."}
```

### 1-4. H3 — 테스트 0건 모듈에 테스트 도입

| 파일 | 케이스 | 고정한 것 |
|---|---:|---|
| `auth/…/UserValidatorTest` | 5 | PLATFORM_ADMIN 부여 차단, 403, **"존재하는가"와 "부여 가능한가"의 구분** |
| `auth/…/AuthServiceTest` | 14 | **생성·수정 양쪽** 차단(한쪽만 막으면 우회), 자기 계정 권한 상승, `createPlatformAdmin`이 의도된 유일한 예외, 교차 테넌트 |
| `auth/…/RoleServiceTest` | 3 | 역할 목록이 부여 가능한 것만 담는다 |
| `contract/…/ContractServiceTest` | 9 | **남의 사업장에 계약을 매다는 경로** 차단, 조회·수정·삭제·목록·통계 격리 |
| `storage/…/DocumentServiceTest` | 16 | 버전 번호 부여, 마지막 버전 삭제 불가, `latestVersionNo` 하향 조건, 메타→파일 순서, 부모 경유 격리 |
| `admin/…/MemberControllerTest` | 7 | 전 경로에서 `principal.getTenantId()`가 Command에 실리는지 |

Fake 6개는 규칙 14대로 **tenant 필터와 예외 규약을 실제 어댑터 그대로 재현**했습니다 —
Fake가 tenant를 무시하면 격리 테스트가 통과해버리므로 이 부분이 검증의 근거입니다.

모듈별 테스트 파일: auth 0→3, contract 0→1, admin 0→1, storage 1→2.

> **완전 해소는 아닙니다.** `dashboard`·`platform`·`global`은 여전히 0건입니다 → M5.

### 1-5. 역할 목록에서 PLATFORM_ADMIN 제외

`GET /api/roles`가 DB의 6개 역할을 전부 내려주고, 프론트엔드(`use-register-member.ts`,
`use-update-member.ts`)가 이를 **필터 없이** 드롭다운으로 만들고 있었습니다. 즉 회원 등록·수정 화면에
"플랫폼 운영자"가 선택지로 떠 있고, 고르면 저장 시점에 403이 났습니다.

부여 차단(보안 경계)은 이미 서버에 있었으므로 권한 상승은 없었지만, **선택지 노출도 서버가 고쳤습니다** —
규칙이 `Role.isPlatformAdmin()` 한 곳에 있고, 클라이언트 필터링은 클라이언트 수만큼 규칙을 복제하기
때문입니다. `RoleService.getRoleList()` → `getAssignableRoles()`로 개명하며 필터를 넣었고,
**엔드포인트와 응답 형태가 그대로라 프론트엔드는 손대지 않았습니다.**

### 1-6. admin의 Member 중간 계층 제거

`domain/Member` + `Create/UpdateMemberCommand` + `MemberService` + `MemberPortMapper` (123줄)를
걷어냈습니다. **admin에서 `application/`과 `domain/` 패키지가 통째로 사라져 `presentation/`만 남습니다.**

| 근거 | 내용 |
|---|---|
| 격리가 아니라 복제였다 | `Member`는 `UserSummary`와 필드 동일(`id`↔`userId`), 행위 0. Command 2개도 auth의 것과 사실상 동일 |
| 조율할 것이 없었다 | `MemberService` 5개 메서드가 전부 단순 위임 |
| 주장된 이점이 성립하지 않았다 | MapStruct 자동 매핑이라 auth에 필드가 늘면 **양쪽을 고쳐야** 했고, `unmappedTargetPolicy` 기본값이라 누락이 조용히 지나갔다 |
| 같은 모듈이 이미 반대 방식 | 문서 관리는 처음부터 중간 계층 없이 동작 중이었다 |

새 `MemberMapper`는 `unmappedTargetPolicy = ERROR`입니다 — auth가 `UserSummary`에 필드를 더하면
컴파일이 깨져 응답 계약을 함께 검토하게 됩니다. 제거 근거였던 함정을 새 코드에 남기지 않으려는 것입니다.

문서 갱신: 루트 `CLAUDE.md`(공유 커널 목록 + 규칙 3에 **"감싸기만 하는 매퍼는 두지 않는다"** 명문화),
`auth/.claude/CLAUDE.md`(공유 커널 노출 범위 선언), `admin/.claude/CLAUDE.md`(제거 근거와 **되살릴 조건**).


### 1-7. H1 — `global`의 모듈 경계 관통 해소 **(3회차 이월 항목)**

`CustomUserDetailsService`가 `auth`·`platform`의 JPA 엔티티와 Spring Data 리포지토리를 직접 들고
있던 문제입니다. 이제 **두 모듈의 `port/in`만 봅니다.**

```
이전                                          이후
CustomUserDetailsService                      CustomUserDetailsService
  → auth.infrastructure.UserJpaRepository       → auth: UserCredentialQueryUseCase
  → auth.infrastructure.UserEntity/RoleEntity   → platform: TenantQueryUseCase
  → platform.infrastructure.TenantJpaRepository
```

- `auth`에 `UserCredentialQueryUseCase` + `UserCredentialSummary`를 신설했습니다.
  **암호화된 비밀번호를 담는 유일한 공개 VO**라 일반 조회 계약(`UserQueryUseCase`)과 분리했습니다.
- **테넌트 이름은 그 VO에 넣지 않았습니다.** auth가 테넌트를 조회하면 `platform → auth` 의존과
  맞물려 순환이 됩니다. 두 모듈의 값을 합치는 일은 어느 쪽도 아닌 `global`이 합니다.

**드러난 빈 순환 2건을 원인 제거로 해결했습니다**(`@Lazy`로 덮지 않았습니다).

| 순환 | 원인 | 조치 |
|---|---|---|
| `SecurityConfig → JwtAuthenticationFilter → … → AuthService → BCryptPasswordEncryptor → PasswordEncoder(SecurityConfig)` | 필터 체인과 무관한 `PasswordEncoder` 빈이 `SecurityConfig`에 있었음 | `PasswordEncoderConfig`로 분리 |
| `AuthService → Authenticator → CustomUserDetailsService → PlatformService → UserCommandUseCase → AuthService` | `PlatformService`가 auth에 의존하면서 조회 계약까지 겸함 | `TenantQueryService`로 공개 계약 구현 분리 |

두 번째는 루트 `CLAUDE.md`의 Service 분리 규칙에 **"공개 계약 구현 분리"** 예외로 명문화했습니다 —
CQRS 축 분할이 아니라 *의존이 다른 한 덩어리*를 갈라내는 것입니다.

검증: 기동 성공 후 로그인 → ADMIN·PLATFORM 경로 접근(200) → 잘못된 비밀번호(401) → 없는 계정(401)을
실 API로 확인했습니다. `CustomUserDetailsServiceTest` 5케이스로 `ROLE_` 접두어 부여와
`UsernameNotFoundException` 규약을 고정했습니다(**global 첫 테스트**).

### 1-8. M2 — `UserService.getUserList`의 N+1 제거

사용자마다 `roleRepository.findById`를 부르던 것을 역할 전체를 한 번 읽어 `Map`으로 붙이도록 바꿨습니다.
목록이 커져도 조회는 2회입니다. 역할은 전역 마스터라 건수가 적어 메모리 부담이 없습니다.

### 1-9. M4 — 구조 정리 완료

- **`domain/port/` 6개 이관** — `auth`의 `Authenticator`·`PasswordEncryptor`·`RoleRepository`·
  `TokenIssuer`·`TokenParser`·`UserRepository`를 `application/port/out/`으로. **패키지가 사라졌고,
  이로써 전 모듈의 Outbound Port 위치가 표준으로 정렬됐습니다.**
- **어셈블러 2건 이관** — `TeamAssembler`(client_management), `ContractDetailAssembler`(contract)를
  각 모듈의 `application/service/assembler/`로. `service/` 직하에는 `@Service`만 남습니다.

### 1-10. L1·L2 — 중복·죽은 코드 제거

- `build.gradle`의 `mapstruct-processor` 중복 선언 제거(42·48행 → 42행만)
- 참조 0인 `ErrorCode` 3건 삭제(`SCHEDULE_ANALYSIS_ALREADY_EXISTS`·`SCHEDULE_ANALYSIS_NOT_FOUND`·
  `SCHEDULE_REOPEN_FORBIDDEN`)

---

## 2. 남은 발견 — 중간

### M1. 조회 조건을 DB로 내리지 않고 전건을 메모리에서 집계 `[성능]`

두 모듈 8개 메서드에 퍼져 있습니다. 선행 리포트의 N4(`contract` 통계)와 같은 뿌리이며, 그 뒤 `schedule`로 번졌습니다.

| 위치 | 문제 |
|---|---|
| `ScheduleStatisticsService:34,41,49` | 완료 건수·월별 집계를 전건 로드 후 자바에서 셈 |
| `ScheduleService:146,155` | 목록/취소목록을 상태 무관 전건 로드 후 필터 |
| `ScheduleService:164` (`toListItems`) | **MongoDB 스냅샷 전건**을 매 목록 조회마다 로드 |
| `ContractService:99,109` | 월별 건수·만료 예정 계약을 전건 로드 후 필터 |

**대시보드 요약 API 한 번에 측정계획 전건이 2회 로드됩니다**(`DashboardService:58,59`). 월별 차트까지 3회.

**제안** — `COUNT`/`WHERE`를 포트로 내립니다(`countByStatusAndTenantId`, `findAllByStatusNot`,
`countByCompletionDateBetween`). `toListItems`는 `findAllByScheduleIdIn(ids, tenantId)`로 필요한 스냅샷만.

### M2. `tenant_id` 인덱스 누락 `[성능]`

| 엔티티 | 영향 |
|---|---|
| `UserEntity` | `findAll(tenantId)` 사용자 목록이 풀스캔. 로그인은 username 유니크 인덱스로 커버됨 |
| `ContractEntity` | `findAllByTenantId`가 풀스캔 — M1과 겹쳐 이중 부담 |

나머지 tenant 종속 엔티티(client_management 9, schedule 2, storage 2)는 인덱스가 있습니다.

### M3. 테스트가 아직 0건인 모듈 `[테스트]`

| 모듈 | 테스트 | 소스 |
|---|---:|---:|
| `dashboard` | 0 | 14 |
| `platform` | 0 | 23 |

`platform`은 **테넌트 발급이 초기 ADMIN 계정 생성까지 함께 하는 원자 경로**라 우선순위가 높습니다.
`dashboard`는 M1을 고칠 때 집계 결과가 바뀌지 않는지 잡아 줄 테스트가 함께 필요합니다.

---

## 3. 남은 발견 — 낮음

| # | 항목 | 내용 |
|---|---|---|
| L1 | 응답 DTO의 UI 용어 **이월** | `MeasurementCountChartResponse`, `FulfillmentBoardResponse` — 규칙 7이 금지한 `Chart`·`Board`. 다만 `FulfillmentBoard`는 도메인 용어로 굳어진 면이 있어 판단 필요 |
| L2 | 페이징 전무 | `Pageable`/`Page` 사용 0건. 측정계획·계약처럼 계속 쌓이는 목록은 결국 필요합니다 |
| L3 | 고아 파일 점검 수단 | 이번에 스크립트로 찾아낸 대조(DB `storage_key` ↔ 디스크)를 반복할 방법이 없습니다. `docs/migration/`에 점검 스크립트를 두거나 운영 도구로 만들 것을 권합니다 |

---

## 4. 규칙 준수 — 전수 검색 위반 0건

| 규칙 | 결과 |
|---|---|
| 1. 계층 의존성 (domain→spring/jpa/application/infra, application→infra, presentation→infra) | **0건** |
| 1. **`global` → 모듈 infrastructure** | **0건** ← 이번 회차 해소 |
| 4. 포트 위치 (`application/port/` 직하 파일 금지, `domain/port/` 잔존) | **0건** ← 이번 회차 해소 |
| 5. 생성자 주입 (`@Autowired` 필드 금지) | **0건** |
| 6. `ApiResponse` 반환 | **정확히 문서화된 5건**(다운로드 4 + SSE 1) |
| 7. Optional 반환 포트 | 1건(`TenantRepository.findByBizNumber`) — 부트스트랩의 "없으면 생성" 용도로 정당하나 **포트 javadoc에 근거 없음** |
| 10. Validator에 `@Transactional` 금지 | **0건** |
| 10. `validate*` 메서드명 금지 | **0건** |
| 13. 컨트롤러가 principal을 받고 미사용 | **0건** |
| `service/` 직하 비-`@Service` | **0건** ← 이번 회차 해소 |
| Service 분할 기준(400줄/public 15개) | **0건** — 최대 `ScheduleSnapshotService` 216줄 |
| JPA `FetchType.EAGER`, `@OneToMany`/`@ManyToMany` | **0건** — 연관을 `@ManyToOne(LAZY)`로만 씀 |
| `TODO`/`FIXME` | 1건(`InspectionPolicy:20`, 도메인 값 확인 요청) |

---

## 5. 선행 리포트(2026-08-25) 이월 대조

| 항목 | 상태 |
|---|---|
| 4-1. `global`의 모듈 JPA 관통 | ✅ **해소** (1-7) |
| 4-2. `domain/port/` 잔존 | ✅ **해소** (1-9) |
| 4-3. 어셈블러 위치 통일 | ✅ **해소** (1-9) |
| 4-4. `command/event/` → `application/event/` | ✅ 해소 |
| 4-5. presentation 구조 | ⚠️ 부분 — 애그리거트별 그룹핑 적용됨 |
| 4-6. `ScheduleService` 632줄 분해 | ✅ 해소 — 171줄 + 유스케이스 축 3개 |
| 4-7. 응답 DTO의 UI 용어 | ❌ 미해소 → L1 |
| 4-8. ErrorCode 정리 | ✅ **해소** (1-10) |
| 4-9. `build.gradle` 중복 | ✅ **해소** (1-10) |
| N2. `AuthService`의 `IllegalArgumentException` | ✅ **해소** (1-3) |
| N4. `contract` 통계 메모리 집계 | ❌ 미해소 → M1 |
| (신규) auth 모듈 테스트 부재 | ✅ **해소** (1-4) |

---

## 6. 권장 조치 순서

1. **M3(platform)** — 테넌트 발급이 초기 ADMIN 계정 생성까지 하는 원자 경로부터. 이번에 `TenantQueryService`를
   분리하면서 `PlatformService`가 발급 유스케이스만 갖게 되어 테스트 대상이 선명해졌습니다.
2. **M1 + M2 + M3(dashboard)** — 포트에 집계·조건 메서드를 내리고 인덱스를 붙입니다.
   dashboard 테스트를 함께 만들면 집계 결과가 바뀌지 않는지 확인하면서 진행할 수 있습니다.
3. **L1** — `FulfillmentBoardResponse`가 UI 용어인지 도메인 용어인지 팀 합의가 필요합니다.
4. **L2 · L3** — 페이징과 고아 파일 점검. 데이터가 쌓이기 시작하는 시점에 맞춥니다.

---

## 7. 이 리포트가 다루지 않은 것

- **프론트엔드**(`ems-web`) — 역할 드롭다운 사용처만 확인했고, 나머지는 대상 밖입니다
- **Spring Security 설정의 인가 규칙 전수 검증** — 경로별 권한 매핑은 별도 점검이 필요합니다
  (이번에 로그인·인가 경로는 실 API로 확인했지만, 전체 매핑 검증은 아닙니다)
- **MongoDB 스키마·인덱스** — `schedule_documents`, `equipments`의 인덱스 설계
- **부하·응답시간 측정** — M1·M2는 코드 형태로 판단했을 뿐 실측하지 않았습니다

### 점검 방법

전수 grep(계층 의존성·주입·포트·응답·검증 규약), 파일 크기 산출, 엔티티 인덱스 선언 대조,
개발 DB 실데이터 대조(`document_versions.storage_key` ↔ `data/storage`), 프론트엔드 사용처 확인,
선행 리포트 이월 항목 재검증. 조치분은 **단위 테스트(452 케이스) + 실 API 왕복**으로 검증했습니다.
