# 프로젝트 가이드라인 (CLAUDE.md)
이 파일은 Claude Code(claude.ai/code)가 이 저장소에서 작업할 때 따라야 하는 **전역 개발 규칙**을 정의합니다.

## 프로젝트 원칙
- **헥사고날 아키텍처(Ports & Adapters) + Domain Driven Design**를 기반으로 기능 모듈 단위로 구성됩니다.
- 모든 응답은 **한국어**로 작성합니다.
- 아키텍처 상세내용은 `ARCHITECTURE.md`를 참고합니다.
- 앱 실행 후 API 문서는 `/swagger-ui.html`에서 확인할 수 있습니다.
- **프론트엔드 저장소**: 이 백엔드와 연동되는 프론트엔드 코드 root는 `C:\dev\projects\new\ems-web` (Vite + React + TypeScript, 별도 저장소)입니다. API 연동 규격·요청/응답 형태 확인이 필요할 때 참조합니다.

## 기술 스택
- **Java** 21
- **Spring Boot** 3.5.x
- **Spring Security** + JWT (JJWT 0.12.x)
- **Spring Data JPA** + MySQL
- **Spring Data MongoDB**
- **Spring Data Redis** (Refresh Token 저장소)
- **MapStruct** 1.6.x
- **jxls-poi** 3.1.0 (엑셀 성적서 출력)
- **Lombok**
- **SpringDoc OpenAPI** 2.8.x (Swagger UI: `/swagger-ui.html`)
- **테스트**: JUnit 5 + AssertJ (`spring-boot-starter-test`)

> **폴리글랏 저장소입니다.** MySQL만 쓰는 프로젝트가 아닙니다.
> MySQL은 원장·메타를, MongoDB는 스키마가 유동적인 문서형 세부 데이터를 담습니다.
>
> | 저장소 | 사용 모듈 | 대상 |
> |---|---|---|
> | MySQL | 대부분 | 원장 테이블 전반 (`docs/DATABASE.md` 참고) |
> | MongoDB | `equipment` | `equipments`, `equipment_inspection_records` |
> | MongoDB | `schedule` | `schedule_documents`, `analysis_records` |
> | Redis | `auth` | Refresh Token |
>
> 두 저장소에 걸친 저장은 2PC를 쓸 수 없으므로 **순서로 정합성을 확보**합니다.
> `ARCHITECTURE.md`의 "폴리글랏 저장소" 절을 참고하세요.

---

## 모듈 지도

기능 모듈 9개 + 공통 인프라(`global`)로 구성됩니다.

| 모듈 | 역할 | 모듈 문서 |
|---|---|---|
| `auth` | 인증·인가, 사용자·역할, JWT/Refresh Token | `auth/.claude/CLAUDE.md` |
| `admin` | 테넌트 관리자용 회원·문서 관리 (자체 원장 없음) | `admin/.claude/CLAUDE.md` |
| `client_management` | 의뢰기관·사업장·측정시설과 하위 설비·측정물질·측정팀 | `client_management/.claude/CLAUDE.md` |
| `contract` | 계약 관리 | `contract/.claude/CLAUDE.md` |
| `dashboard` | 통계·요약 조회 전용 (자체 원장 없음) | `dashboard/.claude/CLAUDE.md` |
| `equipment` | 측정 장비와 검사 이력 (MongoDB) | `equipment/.claude/CLAUDE.md` |
| `platform` | 플랫폼 운영자의 고객사(테넌트) 생명주기 관리 | `platform/.claude/CLAUDE.md` |
| `schedule` | 측정계획·측정 시트·실험분석정보·주기 이행 이력 (MySQL + MongoDB) | `schedule/.claude/CLAUDE.md` |
| `storage` | 문서 저장·버전 관리 | `storage/.claude/CLAUDE.md` |
| `global` | 공통 인프라: 보안 설정, Swagger, 공유 enum, `ApiResponse`, 예외 | — |

작업 전에 해당 모듈의 `.claude/CLAUDE.md`를 먼저 읽습니다. 루트 규칙과 충돌하면 루트가 우선하며,
모듈 문서가 **명시적 예외**로 근거와 함께 선언한 것만 예외입니다.

---

## 전역 개발 규칙

### 1. Layer 의존성 규칙
의존성 방향은 반드시 다음을 따릅니다.

    presentation → application → domain
    domain ← infrastructure

**허용되지 않는 규칙**
- presentation → infrastructure 직접 참조
- application → infrastructure 직접 참조
- domain → spring framework 의존
- domain → jpa 의존
- domain → infrastructure 의존
- domain → application 의존 (Command·VO를 domain이 참조하지 않습니다)

#### 공유 커널 (명시적 예외)

공급 모듈이 `application/port/in/` 공개 계약에 **노출한 도메인 타입은 소비 모듈이 직접 참조할 수 있습니다.**
포트 시그니처에 이미 드러난 타입을 다시 감싸면 변환 계층만 늘고 얻는 것이 없기 때문입니다.

- 현재 해당하는 것: `equipment`의 `EquipType`·`InspectionType`·`InspectionItem`·`EquipmentSpec`(sealed)과
  그 하위 구체 타입(`NozzleSpec`·`PitotTubeSpec`·`ParticleSamplerSpec` 등). `schedule`·`dashboard`가 참조합니다.
- 노출 범위는 **공급 모듈의 문서가 선언**합니다(`equipment/.claude/CLAUDE.md`). 선언되지 않은 타입은 참조하지 않습니다.
- **역방향은 금지입니다** — 공급 모듈이 소비 모듈을 참조해서는 안 됩니다.
- 도메인 타입이 아니라 **데이터**(이름·상태 등)가 필요하면 공유 커널이 아니라 `port/in` 조회로 가져옵니다.

### 2. 신규 기능 추가 시
기능은 반드시 **기능 모듈 단위**로 추가합니다.

    Ex)
    - auth
    - client_management
    - schedule

- global에 기능성 코드를 추가하지 않습니다.
- global은 공통 인프라만 관리합니다.

#### `global/common/enums/` (명시적 예외)

**둘 이상의 모듈이 공유하는 도메인 enum**에 한해 `global/common/enums/`를 허용합니다(공유 커널).
한 모듈만 쓰는 enum은 그 모듈의 `domain/`에 둡니다.

| enum | 공유 모듈 |
|---|---|
| `Grade`·`MeasurementCycle`·`MeasurementField`·`MeasurementMethod`·`Orientation`·`PollutantPhase`·`Shape` | client_management + schedule |
| `DocumentCategory` | admin + storage |

#### 이벤트 패키지

모듈 내 이벤트와 그 리스너는 **`application/event/`**에 둡니다. 모듈 내부용이든 타 모듈이 소비하든 위치는 같습니다.

- 타 모듈이 소비하는 이벤트는 **공개 계약**입니다. 발행 모듈이 계약을 바꿀 때 소비자를 확인할 책임을 집니다.
  (현재 크로스모듈 이벤트는 `client_management`의 `WorkplaceDeletedEvent` 하나이며 `contract`가 소비합니다.)
- 이벤트 타입명은 `{대상}{과거형동작}Event`입니다 (`WorkplaceDeletedEvent`, `SheetsSavedEvent`).

### 3. Mapping 규칙
- 객체 변환은 `MapStruct`를 사용합니다.
- 매퍼는 아래 네 범주 중 하나이며, 그 외 위치에 두지 않습니다. 컨트롤러나 서비스에서 직접 매핑하지 않습니다.

| 위치 | 범주 | 클래스명 | 변환 |
|---|---|---|---|
| `presentation/**/mapper/` | 요청·응답 매퍼 | `{도메인}Mapper` | Request → Command, Domain·VO → Response |
| `infrastructure/mapper/` | 영속 매퍼 | `{도메인}EntityMapper` (Mongo는 `{도메인}DocumentMapper`) | JPA 엔티티·문서 ↔ 도메인 |
| `application/mapper/` | **인터모듈 매퍼** | `{대상}PortMapper` | 타 모듈 `port/in` DTO ↔ 자기 도메인·커맨드 |
| `application/mapper/` | **포트 계약 생산 매퍼** | `{대상}SummaryMapper` | 자기 도메인·VO → 자기 `port/in` 공개 계약 |

- 애그리거트가 여러 개인 모듈은 `presentation/{aggregate}/mapper/`로 애그리거트별 그룹핑합니다.
- **인터모듈 매퍼**는 소비 모듈이 공급 모듈의 `port/in` 계약과만 결합하도록 하는 경계 변환입니다.
  - 예) `admin/application/mapper/MemberPortMapper` — auth의 `UserSummary`→admin `Member`, admin `CreateMemberCommand`→auth `CreateUserCommand`
- **포트 계약 생산 매퍼**는 방향이 반대입니다. 자기 모듈이 외부에 공개할 `~Summary`를 만드는 변환이라
  presentation도 infrastructure도 아니며, 같은 이유로 `application/mapper/`에 둡니다.
  - 예) `contract/.../ContractSummaryMapper`, `equipment/.../InspectionDueSummaryMapper`, `platform/.../TenantSummaryMapper`

> **MapStruct 예외**: 변환 대상이 record가 아니고(빌더·getter 클래스) 조건 분기·리스트 결합이 얽힌 경우
> `@Component` 순수 자바 매퍼를 허용합니다. 현재 `schedule`의 `ScheduleExportViewMapper`·`SheetExportViewMapper`
> 2건이며 근거는 각 클래스 javadoc에 있습니다.

#### 매퍼 메서드 명
- `to{동작}Command()` — Request → Command. **동작을 명시합니다**
  (`toCreateCommand`, `toUpdateCommand`, `toReorderCommand`, `toSaveAnalysisResultsCommand`)
- `toResponse()` / `toResponses()` — Domain·VO → Response DTO
- `toListResponse()` / `toListResponses()` — 목록 아이템 VO → List Response DTO
- `toEntity()` — Domain → JPA 엔티티 / `toDocument()` — Domain → MongoDB 문서
- `toDomain()` / `toDomainList()` — JPA 엔티티·문서 → Domain

> **Command 매퍼 규칙**: 단일 Command만 존재하는 도메인이라도 `toCommand()`가 아니라 동작을 명시합니다.
> 어떤 유스케이스로 가는 변환인지가 이름에서 드러나야 합니다.

### 4. Repository 규칙
Application Service는 Spring Data Repository를 직접 사용하지 않습니다.
반드시 Outbound Port를 통해 접근하며, Outbound Port의 표준 위치는 `application/port/out/`입니다.

    Ex)
    ClientRepository (application/port/out) → ClientRepositoryAdapter → ClientJpaRepository

> **포트 위치 표준화**: Repository 등 Outbound Port는 `application/port/out/`, 외부 공개 Inbound Port(UseCase)는
> `application/port/in/`에 둡니다. `application/port/` **직하에는 파일을 두지 않습니다.**
>
> `domain/port/`는 레거시 위치이며 **auth 6개**(`Authenticator`·`PasswordEncryptor`·`RoleRepository`·
> `TokenIssuer`·`TokenParser`·`UserRepository`)와 **contract 1개**(`ContractRepository`)가 남아 있습니다.
> 신규 포트는 `domain/port/`에 만들지 않습니다.

### 5. 생성자 주입
Lombok `@RequiredArgsConstructor`를 통한 생성자 주입만 사용합니다. 필드 주입은 사용하지 않습니다.

### 6. 응답 규칙
모든 엔드포인트는 `global/web/`의 `ApiResponse<T>`를 반환합니다.

**예외 — 봉투를 씌울 수 없는 응답**

바이너리 다운로드와 스트리밍은 `ApiResponse<T>`로 감싸지 않습니다. 현재 5곳이며,
예외를 늘릴 때는 해당 컨트롤러 javadoc에 근거를 남깁니다.

| 반환 타입 | 위치 |
|---|---|
| `ResponseEntity<byte[]>` | `storage/.../DocumentController` — 문서 다운로드 2개 |
| `ResponseEntity<byte[]>` | `schedule/.../ScheduleExportController` — 성적서 xlsx, 채취기록부 ZIP |
| `ResponseEntity<SseEmitter>` | `schedule/.../ScheduleStreamController` — 측정 시트 편집 알림 |

> 반환 타입 선언은 **실제 응답 본문과 일치**해야 합니다. 본문 없이 `ApiResponse.success()`만 반환한다면
> 선언도 `ApiResponse<Void>`여야 합니다. 불일치하면 Swagger 스키마가 거짓말을 합니다.

### 7. 네이밍 컨벤션

#### 클래스 명
| 구분 | 패턴 | 예시 |
|------|------|------|
| Controller | `{도메인}Controller` | `ClientController` |
| Service | `{도메인}Service` | `ClientService` |
| Validator | `{애그리거트}Validator` | `StackValidator`, `TeamValidator` |
| Domain 모델 | 단순 명사 | `Client`, `User` |
| Outbound Port | 역할 기반 명사 | `ClientRepository`, `TokenIssuer` |
| Inbound Port (UseCase) | `{도메인}{동작}UseCase` | `WorkplaceQueryUseCase` |
| Command | `{동작}{대상}Command` (Record) | `CreateClientCommand`, `CreateUserCommand` |
| VO (결과값) | `{대상}{종류}` (Record, 아래 접미사 표 참고) | `SignInResult`, `WorkplaceListItem`, `StackDetail`, `UserSummary` |
| Request DTO | `{동작}{대상}Request` | `CreateClientRequest` |
| Response DTO | `{대상}Response` | `ClientResponse`, `WorkplaceListResponse` |
| JPA 엔티티 | `{도메인}Entity` | `ClientEntity` |
| MongoDB 문서 | `{도메인}Document` | `ScheduleDocument` |
| JPA Repository | `{도메인}JpaRepository` | `ClientJpaRepository` |
| Repository Adapter | `{도메인}RepositoryAdapter` | `ClientRepositoryAdapter` |
| 기타 Adapter | 기술+역할 | `BCryptPasswordEncryptor`, `JwtTokenIssuer` |
| Presentation 매퍼 | `{도메인}Mapper` | `ClientMapper` |
| Infrastructure 매퍼 | `{도메인}EntityMapper` | `ClientEntityMapper` |

> **Response DTO 주의**: 이름에 UI 컴포넌트(`Table`, `Grid`, `Card`, `Chart`, `Board` 등)를 포함하지 않습니다.
> 동일한 응답이 다양한 UI로 렌더링될 수 있으므로 용도가 아닌 도메인 개념으로 명명합니다.
> 예) `WorkplaceTableListResponse` (X) → `WorkplaceListResponse` (O)
>
> 단 `ContractTableViewEntity`의 `TableView`는 `@Subselect` **DB 뷰**를 가리키는 read-model 엔티티명이므로
> 이 규칙의 대상이 아닙니다.

#### Application 협력자 (Service 외)

유스케이스 자체가 아니라 **서비스가 위임하는 협력자**입니다. 전부 `@Component`이며
**`@Transactional`을 붙이지 않습니다** — 호출하는 Service의 트랜잭션에 참여합니다 (Validator와 같은 규약).

| 접미사 | 책임 | 예시 |
|---|---|---|
| `{대상}Assembler` | 여러 포트를 모아 조회 VO로 조립 | `StackDetailAssembler`, `ScheduleSnapshotAssembler`, `TeamAssembler` |
| `{대상}Recorder` | 유스케이스 완료 시 파생 이력 기록 | `MeasurementRecordRecorder` |
| `{대상}Finder` | 단순 조회를 넘는 탐색 규칙 캡슐화 | `PreviousSheetFinder` |
| `{대상}Indexer` | 두 애그리거트의 결합 규칙 캡슐화 | `AnalysisRecordIndexer` |
| `{대상}Recalculator` | 도메인 계산 엔진과 애그리거트 사이의 어댑터 | `SnapshotSheetRecalculator` |

- `{대상}Detail`을 반환하는 어셈블러만 `{대상}DetailAssembler`로 씁니다 (`StackDetailAssembler`, `ContractDetailAssembler`).
- **위치는 `application/service/assembler/`**입니다. (현재 `service/` 직하에 남은 것들이 있으며 순차 이관 예정입니다.)
- 협력자로 뽑는 기준은 Validator와 같습니다 — 서비스 본문에 조립·탐색 절차가 남지 않는 것이 목표이며,
  단순 위임 래퍼를 만들기 위한 규칙이 아닙니다.

> **Service 분리 방침**: 유스케이스는 도메인당 단일 `{도메인}Service`로 둡니다. Command/Query 서비스 분리(CQRS)는
> 현재 채택하지 않으며, 규모가 커지면 재검토합니다.
> 예외 — 빈 이름이 충돌하면 모듈명을 씁니다. `platform`의 도메인은 `Tenant`이지만 서비스는 `PlatformService`입니다.

#### DTO·VO 접미사 체계 (Request / Response / Command / VO)
**핵심 원칙: 접미사가 그 타입의 계층과 입·출력 방향을 결정한다.** 파일이 많아져도 접미사만 보면 위치를 판단할 수 있어야 합니다.

| 접미사 | 계층·방향 | 프리픽스 규칙 | 위치 | 예시 |
|--------|-----------|--------------|------|------|
| `~Request` | presentation **입력** | `{동작}{대상}Request` | `presentation/**/request/` | `CreateMemberRequest`, `SignInRequest` |
| `~Response` | presentation **출력** | `{대상}Response`, `{대상}List·Detail`+`Response` | `presentation/**/response/` | `MemberResponse`, `ContractListResponse`, `StackDetailResponse` |
| `~Command` | application **입력**(쓰기 유스케이스 파라미터) | `{동작}{대상}Command` | `application/command/` | `CreateMemberCommand`, `UpdateStackCommand` |
| `~Result` | application **출력**(쓰기 유스케이스 반환 VO) | `{동작}{대상}Result` | `application/command/` | `SignInResult` |
| `~ListItem` | application **출력**(목록 조회 아이템 VO) | `{대상}ListItem` | `application/command/` | `WorkplaceListItem`, `StackListItem` |
| `~Detail` | application **출력**(상세·조립 조회 VO) | `{대상}Detail` | `application/command/` | `StackDetail`, `ContractDetail` |
| `~Summary` | application **출력**, **타 모듈 공개용** | `{대상}Summary` | `application/port/in/` | `UserSummary`, `ContractSummary`, `TenantSummary` |
| `~ExportView` | application **출력**, **외부 템플릿 엔진 바인딩용** | `{대상}ExportView` | `application/command/export/` | `ScheduleExportView`, `SheetExportView` |
| `~Event` | application **알림 페이로드** | `{대상}{과거형동작}Event` | `application/event/` | `WorkplaceDeletedEvent`, `SheetsSavedEvent` |

**프리픽스 2대 원칙**
- **입력 계열(Request/Command/Result)**: 동작을 앞에 — `Create`/`Update`/`SignIn` + 대상. (예: `CreateMemberCommand`)
- **조회 결과 VO(ListItem/Detail/Summary/ExportView)**: 대상을 앞에 — 대상 + 종류접미사. (예: `WorkplaceListItem`)
- 타 모듈 공개 `port/in`의 **Command도 입력 계열이므로 동작을 앞에** 둡니다. (예: `CreateUserCommand` — `UserCreateCommand` (X))

> **`~Result` vs `~Response` 구분 유지**: `~Result`는 application VO, `~Response`는 presentation DTO입니다.
> 계층 경계를 나타내므로 (`SignInResult` → `SignInResponse`) 하나로 합치지 않습니다.

> **`~ExportView`가 별도 접미사인 이유**: jxls의 JEXL이 표준 getter로 프로퍼티를 해석하므로 **record를 쓸 수 없고**(규칙 8의 예외),
> 필드명이 곧 고객이 작성하는 엑셀 템플릿의 계약이라(`docs/excel-template-guide.md`) 함부로 바꿀 수 없습니다.
> 이 두 제약이 다른 VO와 성격이 달라 접미사를 나눕니다.

> **Command 하위 패키지**: 커맨드/VO 파일이 적으면 `application/command/` flat로 둡니다.
> 애그리거트가 많아 파일이 늘면 종류별로 하위 그룹핑합니다.
> `client_management`가 `command/create·update·detail·list_item/`, `schedule`이 여기에 `export/`를 더한 형태입니다.

#### Repository Port 메서드 명

**이 프로젝트는 멀티테넌시입니다.** 조회·수정·삭제 포트 메서드는 tenant 범위를 파라미터로 받습니다(규칙 13 참고).

| 동작 | 메서드 패턴 | 반환 타입 | 예시 |
|------|-----------------------------|---------|------|
| 저장 | `save(T entity)` | `T` | `save(Client client)` |
| PK 단건 조회 | `findById(Long id, Long tenantId)` | `T` | `findById(stackId, tenantId)` |
| 필드 조건 단건 조회 | `findBy{Field}(value, tenantId)` | `T` | `findByUsername(String username)` |
| 부모 ID 기준 목록 조회 | `findBy{ParentId}(Long id, Long tenantId)` | `List<VO>` | `findByClientId(clientId, tenantId)` |
| 전체 목록 조회 | `findAll(Long tenantId)` | `List<T>` | `findAll(tenantId)` |
| 존재 확인 | `existsBy{Field}(value)` | `boolean` | `existsByUsername(String username)` |
| 삭제 | `deleteById(Long id, Long tenantId)` | `void` 또는 `boolean` | `deleteById(id, tenantId)` |

- **전역 리소스는 tenantId를 받지 않습니다.** 현재 3개뿐이며 각각 모듈 문서에 근거가 있습니다 —
  `PollutantCatalogRepository`(전 tenant 공유 측정물질 가이드) · `TenantRepository`(Tenant 자체가 격리 축) · `RoleRepository`(전역 역할 마스터)
- `existsBy...`에 tenant를 넣는 기준은 규칙 13을 참고합니다. 부모 ID를 포함하면 부모가 이미 tenant 종속이므로 넣지 않습니다.

> **단건 조회 반환 타입 규칙**: Port의 `findById` / `findBy{Field}` 는 `Optional` 을 반환하지 않습니다.
> Adapter 구현체에서 `.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND))` 로 처리하고, Port 인터페이스는 `T` 를 직접 반환합니다.

### 8. 도메인 모델 패턴
- **Lombok 조합**: `@Builder(toBuilder = true)` + `@Getter` + `@AllArgsConstructor` + `@NoArgsConstructor`
- **생성 로직**: 생성자 직접 사용 금지. 정적 팩토리 메서드로 비즈니스 의미를 부여합니다.
- **수정 로직**: 도메인 모델에 `update()` 메서드를 추가하고 `toBuilder()`로 새 인스턴스를 반환합니다.
  - null 또는 blank 값은 기존 값을 유지하는 `keep()` 정적 헬퍼를 도메인 내부에 정의합니다.
  - PUT 엔드포인트에서도 전달되지 않은 필드는 기존 값이 유지됩니다.
  ```java
  // 예시 패턴
  public Domain update(String field, ...) {
      return this.toBuilder()
          .field(keep(field, this.field))
          .build();
  }
  private static String keep(String value, String original) {
      return value == null || value.isBlank() ? original : value;
  }
  ```
  - **null의 뜻이 경로마다 다를 수 있습니다.** 항목 표 전체를 보내는 일괄 저장 경로에서는 "미전달 = 지움"이 됩니다.
    새 필드를 추가할 때 어느 시맨틱인지 먼저 정하고 문서화합니다
    (`schedule/.claude/CLAUDE.md`의 "null 시맨틱 규약" 참고).
- **Command / VO**: 불변 값 객체는 Java **Record**로 작성합니다.
  - Command: `application/command/` 패키지
  - VO (결과값): `application/command/` 패키지
    - 순수 도메인 개념의 값 객체만 `domain/` 패키지 사용
    - Application 서비스가 반환하는 쿼리 결과 VO는 반드시 `application/command/`에 위치
    - `application/result/` 등 별도 패키지를 생성하지 않습니다
  - **Record 예외**: 외부 템플릿 엔진이 getter로 프로퍼티를 해석해야 하는 `~ExportView`만
    `@Getter` + `@Builder` 클래스로 둡니다.

### 9. Swagger 어노테이션 규칙
- 모든 Controller 클래스에 `@Tag(name = "...", description = "...")` 어노테이션을 추가합니다.
- 엔드포인트별 `@Operation`, `@ApiResponse` 어노테이션은 선택 사항이며, 복잡한 API에만 추가합니다.

### 10. 검증(Validation) 계층 규칙

검증 책임은 4곳으로 분배합니다. **어느 계층이 담당하는지는 "그 검증에 무엇이 필요한가"로 결정합니다.**

| 검증 종류 | 담당 | 위치 |
|-----------|------|------|
| 형식·필수값·범위 | Bean Validation 어노테이션 | `presentation/**/request/`의 Request DTO |
| **비즈니스 규칙 (포트 조회 필요)** | **`{애그리거트}Validator`** | **`application/validator/`** |
| 단건 존재·tenant 소유권 | `findById(id, tenantId)` + `orElseThrow(NOT_FOUND)` | `infrastructure/adapter/` |
| 외부 의존 없는 도메인 불변식 | 도메인 모델의 `require*()` / 상태 전이 메서드 | `domain/` |

#### Validator 규칙
- 위치는 `{모듈}/application/validator/`, 클래스명은 `{애그리거트}Validator`입니다.
  - `service/`에 두지 않습니다. Service·Assembler와 역할이 다르므로 `mapper/`·`command/`처럼 역할 단위 서브패키지로 분리합니다.
  - `domain/`에 두지 않습니다. port를 주입받아야 하므로 `domain → application` 역방향 의존이 되어 1번 규칙에 위배됩니다.
- `@Component`로 선언하고, 주입받을 의존성이 있으면 `@RequiredArgsConstructor`를 붙입니다.
  **주입 대상은 `application/port/out/`과 타 모듈 `application/port/in/`뿐**입니다.
  Spring Data Repository·JPA 엔티티를 직접 참조하지 않습니다.
  - 서비스가 이미 읽어 온 컬렉션을 받아 대조하는 Validator는 의존성이 0개일 수 있습니다(`FacilityValidator.requireExactOrder`).
- `@Transactional`을 붙이지 않습니다. 호출하는 Service의 트랜잭션에 참여합니다.
- 메서드명은 `require{조건}` — 통과 시 `void`, 실패 시 `CustomException`을 던집니다.
  - `validate*`는 boolean 반환으로 읽히므로 사용하지 않습니다.
  - 도메인 모델의 불변식 메서드도 같은 접두사를 씁니다 (`Schedule.requireEditable()`).

```java
// service — 검증 의도만 남고 조건식은 사라진다
public Stack createStack(CreateStackCommand command) {
    stackValidator.requireUniqueNameInWorkplace(name, workplaceId, field);
    return stackRepository.save(Stack.register(...));
}
```

> **Validator로 뽑는 기준**: 포트 조회가 필요한 규칙(중복 검사, 타 모듈 리소스 존재+tenant 소속 확인, 배치 입력 내부의 자기 중복)만 뽑습니다.
> 서비스 본문에 `if`·임시 컬렉션이 남지 않는 것이 목표이며, 단순 위임 래퍼를 만들기 위한 규칙이 아닙니다.
> 레퍼런스 구현은 `client_management/application/validator/`(9개)입니다.

### 11. 패키지별 규칙
패키지별 세부 규칙은 각 모듈의 `.claude/CLAUDE.md`를 참고합니다. 위치는 위 "모듈 지도" 표에 있습니다.

### 12. ErrorCode 컨벤션

#### 분류
| 분류 | 네이밍 패턴 | 예시 |
|------|------------|------|
| 범용 HTTP 상태 | `{CONDITION}` | `NOT_FOUND`, `CONFLICT`, `BAD_REQUEST` |
| 도메인 특화 비즈니스 규칙 | `{DOMAIN}_{CONDITION}` | `SCHEDULE_ALREADY_EXISTS` |

- 두 분류 모두 `global/exception/ErrorCode.java` 단일 enum에서 관리합니다.
- 도메인 특화 코드는 반드시 도메인 prefix를 붙입니다.
- 인라인 메시지 override가 필요한 경우 → 먼저 도메인 특화 ErrorCode(`{DOMAIN}_{CONDITION}`) 추가 여부를 검토합니다.
  - 정당한 override는 **동적 값을 메시지에 넣어야 할 때**입니다(충돌한 시트 이름 등). 고정 문구라면 ErrorCode를 신설합니다.
- `GlobalExceptionHandler`는 `e.getMessage()`를 클라이언트에 응답하므로, ErrorCode.message는 항상 사용자에게 노출될 문장으로 작성합니다.

#### CustomException 로깅 규칙
- `GlobalExceptionHandler`는 `errorCode.getStatus().is4xxClientError()` 기준으로 로그 레벨을 분기합니다.
  - **4xx** → `log.warn` (클라이언트 실수, 정상 비즈니스 거부)
  - **5xx** → `log.error` (서버 오류, 모니터링 알람 대상)

#### UNAUTHORIZED / FORBIDDEN 사용 주의
- `UNAUTHORIZED`는 토큰 만료·미인증 등 인증 자체가 없는 경우에만 사용합니다.
- 로그인 실패(`BadCredentialsException`)와 혼용하지 않습니다. 두 경로는 의미가 다릅니다.
  - 로그인 실패: `BadCredentialsException` → "아이디 또는 비밀번호가 일치하지 않습니다."
  - 미인증 접근: `CustomException(ErrorCode.UNAUTHORIZED)` → "인증이 필요합니다."

### 13. tenant 소유권 격리 (멀티테넌시)

**모든 aggregate는 로그인 사용자의 tenant 범위 안에서만 조회/수정/삭제됩니다.**
전역 리소스(아래 예외)를 뺀 나머지에서 이 규칙을 벗어난 경로는 곧 교차 테넌트 취약점입니다.

- tenantId는 **오직 `@AuthenticationPrincipal CustomUserDetails`의 `principal.getTenantId()`**에서만 얻습니다.
  path/header/body로 받지 않습니다. 클라이언트가 보낸 tenantId는 신뢰하지 않습니다.
- Controller는 read/update/delete에서 `principal.getTenantId()`를 Service로 **전달만** 하고, 검증 분기(if)는 두지 않습니다.
  - **파라미터로 받아 놓고 쓰지 않는 것이 가장 위험합니다.** 시그니처만 보면 격리된 것처럼 보이기 때문입니다.
- Service는 `(id, tenantId)`를 Port에 전달합니다. update는 반드시 `findById(id, tenantId)`로 소유권을 검증한 뒤 `save()` 합니다.
  create는 부모 aggregate 존재 검증도 `findById(parentId, tenantId)`로 tenant 범위에서 수행합니다.
- Adapter/JpaRepository는 WHERE 절에 `tenant_id`를 포함(`findByXxxIdAndTenant_TenantId`, `findAllByTenant_TenantId`)해
  다른 tenant 행을 애초에 읽지 못하게 합니다. 삭제는 `deleteByXxxIdAndTenantId`로 원자 삭제 후 `deletedCount == 0`이면 `NOT_FOUND`.
- **소유권 불일치는 403이 아니라 404 `NOT_FOUND`**로 처리합니다 — 리소스 존재 자체를 은닉합니다.
- 중복체크(`existsBy...`) 중 부모ID를 포함하는 것(`existsByNameAndClientId` 등)은 부모ID가 이미 tenant에 종속되므로
  tenant 파라미터를 두지 않습니다. 부모 없는 전역 유일 체크는 tenant를 포함합니다(`existsByNameAndTenantId`).
- **목록·통계·SSE 구독도 예외가 아닙니다.** 부모 id를 쿼리 파라미터로 받는 목록 조회는
  그 부모가 호출자 tenant 소속인지 반드시 확인합니다.

#### 전역 리소스 (명시적 예외)

| 대상 | 근거 |
|---|---|
| `PollutantCatalog` (client_management) | 전 tenant가 공유하는 측정물질 가이드. `tenant_id` 컬럼 자체가 없습니다. 관리 API는 `/api/platform/**`로 `PLATFORM_ADMIN` 한정 |
| `Tenant` (platform) | 테넌트가 격리 축 자체입니다. `/api/platform/**`로 보호 |
| `Role` (auth) | 전역 역할 마스터 |

#### 역할 부여 제한

테넌트 격리만으로는 **자기 계정 권한 상승**을 막지 못합니다. 역할 부여는 별도 규칙입니다.

- 테넌트 ADMIN은 `PLATFORM_ADMIN` 역할을 부여할 수 없습니다 — **생성·수정 양쪽 경로 모두**에 적용됩니다.
  (`auth/application/validator/UserValidator.requireAssignableRole` → `ROLE_NOT_ASSIGNABLE`)
- 부트스트랩(`PlatformAdminInitializer`)이 `UserCommandUseCase.createPlatformAdmin`으로 만드는 것이 **유일한 예외 경로**입니다.
- "그 역할이 존재하는가"(`roleRepository.findById`)를 **권한 검증으로 착각하지 않습니다.**

### 14. 테스트 규칙

`src/test`의 사실상의 표준입니다. 새 테스트는 이 스타일을 따릅니다.

- **Fake 리포지토리 패턴이 표준입니다** — Mockito가 아닙니다.
  `src/test/.../{module}/application/Fake{Port}Repository`에 `port/out` 인터페이스를 **인메모리로 직접 구현**합니다.
  - 픽스처 등록용 `given(...)` 메서드 제공, id 자동 채번
  - **실제 어댑터와 동일하게 예외를 던집니다** — `findById` 실패 시 `CustomException(ErrorCode.XXX_NOT_FOUND)`
  - **tenantId 필터링을 그대로 재현합니다.** 그래야 멀티테넌시 격리가 테스트로 검증됩니다
- **서비스는 생성자로 직접 조립합니다.** Spring 컨텍스트를 띄우지 않습니다.
  `new TeamService(fakeRepo, new TeamValidator(...), new TeamAssembler(...))`
  `@SpringBootTest`는 컨텍스트 로드를 확인하는 `EmsApplicationTests` 하나뿐입니다.
- **그 유스케이스가 쓰지 않는 협력자는 mock이 아니라 "호출되면 실패하는" 익명 구현체**로 넘깁니다.
  모든 메서드가 `UnsupportedOperationException`을 던지게 해서 "이 경로는 저기까지 가지 않는다"를 구조로 고정합니다.

  ```java
  private static final UserQueryUseCase UNUSED_USER_QUERY = new UserQueryUseCase() {
      @Override public UserSummary getUser(Long userId, Long tenantId) { throw new UnsupportedOperationException(); }
      // ...
  };
  ```
- **AssertJ 전용** — `assertThat` / `assertThatThrownBy` / `assertThatCode`. JUnit `Assertions.*`를 쓰지 않습니다.
- `@Nested` + `@DisplayName`으로 유스케이스별 그룹핑, 테스트 메서드명은 **한글 스네이크**
  (`void 사수로_배정된_팀의_id와_이름을_반환한다()`). 클래스는 `public` 없이 package-private.
- 클래스 javadoc에 **"무엇을 왜 고정하는가"**를 적습니다.
- 교차 테넌트 격리는 `TENANT`/`OTHER_TENANT` 두 상수로 검증합니다.
- MockMvc는 `MockMvcBuilders.standaloneSetup(controller)`만 사용하며, 용도는 **라우팅 우선순위 회귀 고정**입니다.

> 순수 도메인 로직(계산·상태 전이·병합·판정)의 커버리지가 가장 두텁습니다.
> Repository Adapter·JPA 매핑·Security 통합 테스트는 현재 없습니다.

### 15. 스키마 마이그레이션 정책

**Flyway·Liquibase를 쓰지 않습니다.** `ddl-auto: update`이므로 컬럼·제약·테이블의
**추가는 자동으로 반영되지만 삭제·변경은 반영되지 않습니다.**

- 삭제·제약 변경·데이터 백필은 `docs/migration/{YYYY-MM-DD}-{설명}.{sql|js}`에 수동 스크립트로 남기고
  **배포 전에 실행**합니다.
- 스크립트는 **멱등**이어야 합니다. 재실행해도 안전해야 합니다.
- 스크립트 주석에 **배포 순서**를 명시합니다 (보통 "구버전 중지 → 스크립트 실행 → 신버전 배포").
- MongoDB도 대상입니다. `@Version`을 새로 도입하면 기존 문서에 백필하지 않는 한
  Spring Data가 이를 신규 문서로 오판합니다.
- 레퍼런스: `2026-08-18-schedule-document-version.js`(Mongo `@Version` 백필),
  `2026-08-25-schedule-drop-status-log.sql`(soft delete → 물리 삭제 전환)

---

## 저장소 문서 지도

| 문서 | 성격 |
|---|---|
| `ARCHITECTURE.md` | 계층·포트·모듈 간 통신 등 구조 전반 |
| `docs/DATABASE.md` | **스키마의 단일 진실.** 테이블·컬럼·제약·Enum 값 |
| `docs/migration/` | 수동 실행 DDL·백필 스크립트 (규칙 15) |
| `docs/excel-template-guide.md` | **고객 대상** jxls 템플릿 작성 매뉴얼. `~ExportView`의 필드명이 곧 이 문서의 계약이라 변경하면 배포된 템플릿이 깨집니다 |
| `docs/equipment/*.md` | equipment 도메인 모델 Mermaid 다이어그램 |
| `docs/architecture-audit-*.md` | 아키텍처 규칙 준수 진단 리포트 (날짜별 이력) |

### `.claude/` 자산

| 항목 | 용도 |
|---|---|
| `.claude/commands/마무리.md` | 변경분을 아키텍처 체크리스트로 검토·보고한 뒤 승인받아 커밋·푸시 |
| `.claude/commands/test-push.md` | 테스트 → `.env` 안전검사 → 커밋 메시지 생성 → commit/push 7단계 자동화 |
| `.claude/agents/domain-architect.md` | 구현 **전에** 애그리거트 경계·API 설계를 검토 |
| `.claude/agents/spring-backend-dev.md` | 확정된 설계대로 구현 |
| `.claude/agents/jpa-db-reviewer.md` | JPA 연관관계·cascade·N+1·인덱스·트랜잭션 리뷰 |
| `.claude/agents/test-writer.md` | 테스트 작성 (규칙 14를 따릅니다) |

---

## 빌드·실행

```bash
./gradlew build          # 빌드
./gradlew test           # 전체 테스트
./gradlew bootRun        # 실행
```

> ⚠️ **Gradle은 반드시 WSL(Linux) 안에서 실행합니다.**
> Windows에서 UNC 경로(`\\wsl.localhost\...`)로 실행하면 Gradle FileHasher가 실패합니다.
> `wsl -d Ubuntu-22.04 -- bash -lc "cd /home/<user>/.../ems-server && ./gradlew test"`

## 커밋 메시지 컨벤션

`type: 한국어 요약` 형식을 사용합니다.

| type | 용도 |
|---|---|
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변경 없는 구조 개선 |
| `test` | 테스트 추가·수정 |
| `docs` | 문서 |
| `chore` | 빌드·설정 |
| `style` | 포매팅 |

- `refact:`(오탈자), 브랜치명 스타일(`Refact/...`), prefix 없는 제목은 사용하지 않습니다.
- 본문에는 무엇을 왜 바꿨는지 남깁니다. 파일 목록은 diff가 이미 말해 줍니다.
