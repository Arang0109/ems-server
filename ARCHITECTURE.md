# 전체 프로젝트 구조 (ARCHITECTURE.md)
**헥사고날 아키텍처(Ports & Adapters) + Domain Driven Design**를 기반으로 기능 모듈 단위로 구성됩니다.

```
src/main/java/com/ensolution/ems/
├── admin/                     # 테넌트 관리자용 회원·문서 관리 (자체 원장 없음)
├── auth/                      # 인증·인가, 사용자·역할, JWT/Refresh Token
├── client_management/         # 의뢰기관 / 사업장 / 측정시설(굴뚝) 및 하위 설비·측정물질·측정팀
├── contract/                  # 계약 관리
├── dashboard/                 # 통계·요약 조회 전용 (자체 원장 없음)
├── equipment/                 # 측정 장비와 검사 이력 (MongoDB)
├── platform/                  # 플랫폼 운영자의 고객사(테넌트) 생명주기 관리
├── schedule/                  # 측정계획·측정 시트·실험분석정보·주기 이행 이력 (MySQL + MongoDB)
├── storage/                   # 문서 저장·버전 관리
└── global/                    # 공통: 보안 설정, Swagger, 공유 enum, ApiResponse, 예외
```

모듈별 세부 규칙은 각 모듈의 `.claude/CLAUDE.md`에 있습니다 (`global` 제외 전 모듈 보유).

---

## 헥사고날 아키텍처 핵심 개념

애플리케이션 **도메인**을 중심에 두고, 외부 세계(HTTP, DB, Redis 등)와의 결합을 **Port & Adapter** 패턴으로 격리합니다.

- **Port**: 도메인 또는 애플리케이션이 필요로 하는 기능을 정의한 인터페이스
- **Adapter**: Port를 특정 기술로 구현한 클래스 (Infrastructure 계층에 위치)
- **효과**: 도메인은 Spring, JPA 등 외부 프레임워크에 의존하지 않음

---

## 계층별 역할과 책임

각 기능 모듈은 4개의 레이어로 구성됩니다.

| 레이어 | 패키지 | 책임 |
|---|---|---|
| Presentation | `presentation/` | HTTP 요청 수신, 입력 검증, 응답 직렬화 |
| Application | `application/` | 유스케이스 조율, 트랜잭션 경계 |
| Domain | `domain/` | 핵심 비즈니스 규칙, 도메인 모델 |
| Infrastructure | `infrastructure/` | 기술 구현체 (DB, 외부 API 등) |

### Presentation 계층
- HTTP 요청을 받아 Command 객체로 변환 후 Application 서비스에 위임합니다.
- 도메인 로직 없이 입력 변환과 응답 직렬화만 담당합니다.
- 모든 응답은 `ApiResponse<T>`로 감쌉니다 (바이너리·SSE 예외는 `CLAUDE.md` 규칙 6 참고).

### Application 계층
- 유스케이스(Use Case) 단위로 서비스를 구성합니다.
- Outbound Port를 통해 도메인 객체를 조회하고, 도메인 메서드를 호출하며, 결과를 저장합니다.
- `@Transactional`로 트랜잭션 경계를 관리합니다.
- Spring Data Repository를 직접 참조하지 않습니다.
- 서비스가 위임하는 협력자(Validator·Assembler·Recorder 등)는 `@Transactional`을 갖지 않고
  호출 서비스의 트랜잭션에 참여합니다.

### Domain 계층
- 비즈니스 규칙과 불변식(Invariant)을 캡슐화합니다.
- Spring, JPA 등 외부 프레임워크에 의존하지 않습니다.
- Application 계층(Command·VO)도 참조하지 않습니다.
- 필요한 외부 기능은 Port 인터페이스로 추상화합니다.

### Infrastructure 계층
- Outbound Port를 실제 기술(JPA, MongoDB, Redis, BCrypt, jxls 등)로 구현합니다.
- JPA 엔티티·MongoDB 문서와 도메인 모델 간의 변환을 담당합니다.

---

## Port 유형

헥사고날 아키텍처에서 포트는 **방향**에 따라 두 종류로 구분됩니다.

### Inbound Port (Primary Port) — 모듈이 외부에 공개하는 계약

외부 모듈이나 Presentation 계층이 이 모듈에 "무엇을 해달라"고 요청하는 인터페이스입니다.

| 위치 | 구현체 | 호출자 |
|------|--------|--------|
| `application/port/in/` | Application Service | 외부 모듈의 Service, Controller |

```
WorkplaceQueryUseCase  — 사업장 존재 확인·요약 조회 (외부 모듈 전용)
```

> Inbound Port는 호출자가 실제로 필요한 메서드만 노출합니다 (ISP).
> Repository 전체를 노출하지 않고, 외부 모듈이 필요한 기능만 정의합니다.
>
> 이 모듈이 외부에 공개하는 VO(`~Summary`)도 같은 위치에 둡니다. 공개 계약의 일부이기 때문입니다.

### Outbound Port (Secondary Port) — 모듈이 외부에 요구하는 계약

이 모듈이 외부 기술(DB, Redis 등)에 "무엇을 해달라"고 요청하는 인터페이스입니다.
구현체는 항상 Infrastructure Adapter입니다.

**Repository를 포함한 Outbound Port는 `application/port/out/`에 두는 것을 표준으로 합니다.**
도메인 계층은 순수하게 유지하고, 인프라에 요구하는 계약은 Application 계층이 소유합니다.

| 위치 | 용도 | 예시 |
|------|------|------|
| `application/port/out/` | Repository 등 인프라에 요구하는 Outbound 계약 (표준) | `ClientRepository`, `WorkplaceRepository`, `StackRepository` (client_management) |
| `domain/port/` | 기술 어댑터형 Outbound 계약 (레거시 위치, 정렬 예정) | auth 6개(`UserRepository`·`RoleRepository`·`PasswordEncryptor`·`TokenIssuer`·`TokenParser`·`Authenticator`), contract 1개(`ContractRepository`) |

> **표준화 진행 상황**: `client_management`·`schedule`·`equipment`·`storage`·`platform`은 이관 완료.
> `auth`(6개)·`contract`(1개)만 `domain/port/`에 남아 있으며 향후 `application/port/out/`으로 정렬 예정입니다.
> **`application/port/` 직하에는 파일을 두지 않습니다** — 반드시 `in/` 또는 `out/` 아래입니다.

---

## 모듈 내 패키지 구조

```
{feature}/
├── presentation/
│   └── {aggregate}/         # 애그리거트가 여러 개인 모듈은 애그리거트별로 그룹핑
│       ├── controller/      # {Aggregate}Controller
│       ├── request/         # 요청 DTO
│       ├── response/        # 응답 DTO
│       └── mapper/          # Request/Response ↔ Command (MapStruct)
├── application/
│   ├── service/             # {Aggregate}Service (유스케이스)
│   │   └── assembler/       # {대상}Assembler — 여러 포트를 모아 조회 VO 조립
│   ├── validator/           # {Aggregate}Validator — 포트 조회가 필요한 비즈니스 규칙
│   ├── command/             # Command 객체, 결과 VO (Java Record)
│   │   ├── create/ update/  # 파일이 많은 모듈은 종류별 하위 그룹핑
│   │   ├── detail/ list_item/
│   │   └── export/          # ~ExportView (jxls 바인딩용, record 아님)
│   ├── event/               # 도메인·알림 이벤트와 그 리스너
│   ├── mapper/              # 인터모듈 매퍼 · 포트 계약 생산 매퍼
│   └── port/
│       ├── in/              # Inbound Port (UseCase) + 공개 VO(~Summary)
│       └── out/             # Outbound Port (Repository 등) — 인프라에 요구하는 계약
├── domain/
│   ├── {Entity}.java        # 도메인 모델 (순수 자바, 프레임워크 비의존)
│   └── {sub}/               # 애그리거트가 크면 하위 그룹핑 (schedule: snapshot·sheet·analysis·history)
└── infrastructure/
    ├── entity/              # {Entity}Entity — JPA 엔티티
    ├── document/            # {Entity}Document — MongoDB 문서
    ├── repository/          # {Entity}JpaRepository / {Entity}MongoRepository
    ├── adapter/             # {Entity}RepositoryAdapter — Port 구현체
    ├── mapper/              # {Entity}EntityMapper / {Entity}DocumentMapper (MapStruct)
    ├── bootstrap/           # {대상}Initializer — 배포 1회성 설치 코드 (계층 규칙의 명시적 예외)
    ├── excel/               # jxls 렌더러 등 기술별 어댑터
    └── sse/                 # SSE 브로드캐스터
```

> **presentation 구조**: 애그리거트가 여러 개인 모듈(`client_management`)은 위와 같이 애그리거트별 서브패키지 아래
> `controller/request/response/mapper`로 중첩합니다. 단일 애그리거트 모듈(`auth`, `admin`, `storage`, `platform`, `dashboard`)은
> `presentation/` 아래에 `controller/`와 `request/response/mapper`를 두는 플랫 구조를 허용합니다.
> **플랫 구조에서도 컨트롤러는 `controller/` 아래**입니다 — `presentation/` 직하에 클래스를 두지 않습니다.

> **`infrastructure/bootstrap/`은 계층 규칙의 명시적 예외입니다.** infrastructure에 있으면서 `@Transactional` +
> 아웃바운드 포트 + 유스케이스를 직접 조합합니다. 런타임 유스케이스가 아니라 배포 1회성 설치 코드이기 때문이며,
> **예외 범위는 `*Initializer` 클래스에 한정**합니다. Spring Data Repository를 직접 주입하지 않고 포트만 씁니다.
> 현재 `PollutantCatalogInitializer`(client_management), `PlatformAdminInitializer`(platform) 2개입니다.

---

## 데이터 흐름

### 쓰기 흐름 (Command)

```
HTTP Request
    ↓
Controller          — Request DTO 수신
    ↓ ({Domain}Mapper)
Command             — 불변 입력 객체 (Java Record)
    ↓
Application Service — 유스케이스 조율, 트랜잭션 관리
    ↓ (Outbound Port)
Domain Model        — 비즈니스 규칙 실행 (정적 팩토리 메서드)
    ↓ ({Domain}EntityMapper)
JPA Entity / MongoDB Document / 외부 기술
```

### 읽기 흐름 (Query)

```
HTTP Request
    ↓
Controller
    ↓
Application Service — Outbound Port로 조회
    ↓ ({Domain}EntityMapper)
Domain Model
    ↓ ({Domain}Mapper)
Response DTO
    ↓
ApiResponse<T>      — HTTP Response
```

---

## 객체 변환 경계

| 변환 위치 | 변환 방향 | 담당 매퍼 | 위치 |
|---|---|---|---|
| Presentation ↔ Application | Request ↔ Command, Domain ↔ Response | `{도메인}Mapper` | `presentation/**/mapper/` |
| Domain ↔ Infrastructure | Domain Model ↔ JPA 엔티티·Mongo 문서 | `{도메인}EntityMapper` / `{도메인}DocumentMapper` | `infrastructure/mapper/` |
| 모듈 ↔ 모듈 | 타 모듈 `port/in` DTO ↔ 자기 도메인·커맨드 | `{대상}PortMapper` | `application/mapper/` |
| 자기 모듈 → 공개 계약 | 자기 도메인·VO → 자기 `port/in` `~Summary` | `{대상}SummaryMapper` | `application/mapper/` |

컨트롤러, 서비스에서 직접 변환하지 않습니다.

---

## 의존성 방향

```
Presentation ──→ Application ──→ Domain
                                   ↑
                 Infrastructure ───┘   (의존성 역전)
```

Infrastructure는 Outbound Port를 구현함으로써 Domain을 향해 의존합니다.  
Domain은 어떤 계층도 참조하지 않습니다.

---

## 폴리글랏 저장소 (MySQL + MongoDB)

원장·메타는 MySQL, 스키마가 유동적인 문서형 세부 데이터는 MongoDB가 담습니다.

| 저장소 | 모듈 | 대상 |
|---|---|---|
| MySQL | 대부분 | 원장 테이블 전반 (`docs/DATABASE.md`) |
| MongoDB | `equipment` | `equipments`, `equipment_inspection_records` — 장비 유형별 사양이 sealed 계층이라 컬럼 스키마와 맞지 않음 |
| MongoDB | `schedule` | `schedule_documents`(측정 시점 스냅샷 + 측정 시트), `analysis_records` |
| Redis | `auth` | Refresh Token |

### 두 저장소에 걸친 애그리거트

`schedule`은 한 모듈 안에서 MySQL 2개 + MongoDB 2개 애그리거트를 다룹니다.
`Schedule`(메타·MySQL)과 `ScheduleSnapshot`(세부·Mongo)은 한 개념의 두 저장소 표현이며 항상 쌍으로 다닙니다.

**2PC를 쓸 수 없으므로 순서로 정합성을 확보합니다.**

- **저장 순서: MySQL → Mongo.** 메타를 진실의 원천으로 두고, 문서 저장을 트랜잭션의 마지막 부수효과로 배치합니다.
- **삭제 순서: Mongo → MySQL** (저장의 역순).
- 상태 전이가 없는 문서 편집은 **문서 단독 쓰기**로 둡니다.
- **알림(SSE)은 커밋 이후에** 발행합니다. 저장 직후 알리면 뒤이어 롤백될 저장까지 "저장됐다"고 알리게 됩니다.
  `TransactionSynchronization.afterCommit`으로 미룹니다.
- 동시 편집이 실제 업무 방식인 영역(측정 시트)은 **문서 단위 낙관적 락 위에 논리 단위 락**을 한 겹 더 둡니다.
  물리적 겹침은 재시도로, 논리적 충돌은 사용자에게 알립니다.

상세는 `schedule/.claude/CLAUDE.md`의 "이중 저장소 정합성 규약"을 참고하세요.

---

## 모듈 간 통신 (Cross-Module Communication)

### 핵심 규칙

다른 모듈의 기능이 필요할 때, 반드시 해당 모듈의 **Inbound Port (application/port/in/)** 를 통해 접근합니다.
Outbound Port (application/port/out/, 레거시는 domain/port/) 를 외부 모듈에서 직접 참조하는 것은 금지입니다.

**이유:**
- `application/port/out/Repository`는 해당 모듈이 인프라에 요구하는 **Outbound 계약**입니다.
  외부에 공개하는 인터페이스가 아니며, 참조 시 모듈 내부 구현 세부사항이 노출됩니다.
- `application/port/in/UseCase`는 해당 모듈이 외부에 공개한 **Inbound 계약**입니다.
  내부 구현이 바뀌어도 이 계약이 유지되는 한 호출자는 영향을 받지 않습니다.

### 올바른 방향 (O)

```
[contract 모듈]
  ContractService
      │
      ▼ (application/port/in/ — Inbound Port)
  WorkplaceQueryUseCase         ← client_management 모듈이 외부에 공개한 계약
      │
      ▼ (구현체)
  WorkplaceService
      │
      ▼ (application/port/out/ — Outbound Port, 모듈 내부)
  WorkplaceRepository
```

### 잘못된 방향 (X)

```
[contract 모듈]
  ContractService
      │
      ▼ (application/port/out/ — Outbound Port를 외부에서 직접 참조)
  WorkplaceRepository           ← 캡슐화 위반: 모듈 내부 계약 노출
```

### Inbound Port 설계 지침

- **좁은 인터페이스**: 호출자가 필요한 기능만 메서드로 정의합니다. Repository 전체를 노출하지 않습니다.
- **의미 있는 이름**: `WorkplaceQueryUseCase`, `WorkplaceCommandUseCase`처럼 역할을 명시합니다.
- **구현체는 Application Service**: `WorkplaceService implements WorkplaceQueryUseCase`
- **공개 VO는 `~Summary`**로 이름 짓고 포트와 같은 위치(`application/port/in/`)에 둡니다.

### 공유 커널 (명시적 예외)

공급 모듈이 `port/in` 계약에 노출한 **도메인 타입**은 소비 모듈이 직접 참조할 수 있습니다.
포트 시그니처에 이미 드러난 타입을 다시 감싸면 변환 계층만 늘기 때문입니다.

- 현재 `equipment`의 `EquipType`·`InspectionType`·`InspectionItem`·`EquipmentSpec`(sealed 및 하위 구체 타입)이
  이에 해당하며 `schedule`·`dashboard`가 참조합니다. 노출 범위는 `equipment/.claude/CLAUDE.md`가 선언합니다.
- 여러 모듈이 공유하는 도메인 enum은 `global/common/enums/`에 둡니다(`CLAUDE.md` 규칙 2).
- **역방향 참조는 금지입니다.**

### 모듈 간 이벤트

- 이벤트와 리스너는 `application/event/`에 둡니다.
- 타 모듈이 소비하는 이벤트는 **공개 계약**이며, 발행 모듈이 계약 변경 시 소비자를 확인할 책임을 집니다.
- 현재 크로스모듈 이벤트는 `client_management`의 `WorkplaceDeletedEvent` 하나이고 `contract`가 소비합니다
  (사업장 삭제 시 계약 캐스케이드).

### 엔티티/모델 참조 규칙 (ID 참조)

모듈 경계를 넘는 참조는 **객체 그래프가 아니라 식별자(ID)** 로 합니다.

- JPA 연관관계(`@ManyToOne`, `@OneToMany`, `@OneToOne`)와 도메인 객체 참조는 **모듈 내부에서만** 사용합니다.
- 다른 모듈의 엔티티/도메인 모델은 직접 참조하지 않고, **`Long workplaceId`처럼 ID 값**으로만 보유합니다.
  - `ContractEntity` → `private Long workplaceId;` (`contract`, client_management 엔티티 직접 참조 금지)
  - `TeamEntity` → 사수·부사수 `userId`, 장비 `equipmentId` (`client_management`, auth·equipment 엔티티 직접 참조 금지)
  - `ScheduleEntity` → `stackId`·`teamId` 전부 plain `Long` (`schedule`은 JPA 연관관계 0개)
- 다른 모듈의 **데이터(이름·상태 등)** 가 필요하면 그 모듈의 Inbound Port로 조회합니다.
  (예: `ContractDetailAssembler`가 `WorkplaceQueryUseCase.getSummaryById()`로 사업장 요약을 가져옴)

**이유:** 모듈 경계를 넘는 객체 그래프 결합은 인프라(JPA·물리 FK) 레벨까지 두 모듈을 묶어
독립 배포·변경을 막습니다. 물리 FK가 필요하면 도메인 결합과 분리해 스키마/마이그레이션 레벨에서 관리합니다.

> **`tenant_id`만은 두 갈래입니다.** 멀티테넌시 앵커라 예외적으로 두 방식이 공존합니다.
> - **JPA `@ManyToOne TenantEntity` + 물리 FK** — clients·workplaces·stacks·facilities·preventions·pollutants·stack_pollutant·teams
> - **plain `Long` 컬럼** — users·contracts·schedules·measurement_records
>
> 전자는 `platform`의 `TenantEntity`를 참조하지만 **infrastructure 계층에 한정**되며, 도메인 모델과
> 포트 시그니처는 양쪽 모두 `Long tenantId`만 씁니다. 현황은 `docs/DATABASE.md`를 참고하세요.

### Tenant 애그리거트의 소유권

`platform` 모듈은 플랫폼 운영자(PLATFORM_ADMIN)가 고객사(테넌트)의 생명주기를 관리하는 모듈입니다.
테넌트 **내부** 업무(사업장·측정시설 등)를 다루는 `client_management`와 관심사가 분리됩니다.

- **`Tenant` 애그리거트 전부를 `platform`이 소유합니다.** 도메인 모델, `application/port/out/TenantRepository`,
  `infrastructure/adapter/TenantRepositoryAdapter`, `infrastructure/mapper/TenantEntityMapper`,
  그리고 **JPA 엔티티(`TenantEntity`)와 `TenantJpaRepository`까지** 모두 `platform`에 있습니다.
- 테넌트 정보가 필요한 모듈은 `platform/application/port/in/TenantQueryUseCase`로 조회하고
  `TenantSummary`를 받습니다. 다른 경로로 `platform` 내부를 들여다보지 않습니다.
- **`TenantEntity`가 멀티테넌시 공용 앵커**라 여러 모듈의 JPA 엔티티가 `@ManyToOne`으로 참조합니다.
  이 참조는 **infrastructure 계층에 한정**되며, 그 위 계층에서는 `Long tenantId`만 오갑니다.
- **`platform` → 다른 기능 모듈 방향의 참조는 두지 않습니다.** `platform`은 테넌트 생명주기만 알면 되고,
  테넌트 안에서 무슨 업무가 일어나는지는 알 필요가 없습니다.
- 같은 이름의 `@Service`를 두 모듈에 두지 않습니다 — 빈 이름이 충돌합니다.
  그래서 `platform`의 서비스는 `TenantService`가 아니라 `PlatformService`입니다.

---

## 멀티테넌시

모든 aggregate는 로그인 사용자의 tenant 범위 안에서만 조회/수정/삭제됩니다.
tenantId는 오직 `@AuthenticationPrincipal`에서 얻고, 소유권 불일치는 404로 은닉합니다.

전체 규칙과 전역 리소스 예외는 **`CLAUDE.md` 규칙 13**을 참고하세요.
이 규칙은 모듈 하나의 관심사가 아니라 전 모듈에 적용되는 전역 규칙입니다.
