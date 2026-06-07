# 전체 프로젝트 구조 (ARCHITECTURE.md)
**헥사고날 아키텍처(Ports & Adapters) + Domain Driven Design**를 기반으로 기능 모듈 단위로 구성됩니다.

```
src/main/java/com/ensolution/ems/
├── auth/                      # 인증 및 JWT
├── client_management/         # 업체 / 사업장 / 굴뚝 관리
└── global/                    # 공통: 보안 설정, Swagger, 공통 enum, ApiResponse
```

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
- 모든 응답은 `ApiResponse<T>`로 감쌉니다.

### Application 계층
- 유스케이스(Use Case) 단위로 서비스를 구성합니다.
- Domain Port를 통해 도메인 객체를 조회하고, 도메인 메서드를 호출하며, 결과를 저장합니다.
- `@Transactional`로 트랜잭션 경계를 관리합니다.
- Spring Data Repository를 직접 참조하지 않습니다.

### Domain 계층
- 비즈니스 규칙과 불변식(Invariant)을 캡슐화합니다.
- Spring, JPA 등 외부 프레임워크에 의존하지 않습니다.
- 필요한 외부 기능은 Port 인터페이스로 추상화합니다.

### Infrastructure 계층
- Domain Port를 실제 기술(JPA, Redis, BCrypt 등)로 구현합니다.
- JPA 엔티티와 도메인 모델 간의 변환을 담당합니다.

---

## Port 유형

헥사고날 아키텍처에서 포트는 **방향**에 따라 두 종류로 구분됩니다.

### Inbound Port (Primary Port) — 모듈이 외부에 공개하는 계약

외부 모듈이나 Presentation 계층이 이 모듈에 "무엇을 해달라"고 요청하는 인터페이스입니다.

| 위치 | 구현체 | 호출자 |
|------|--------|--------|
| `application/port/` | Application Service | 외부 모듈의 Service, Controller |

```
WorkplaceQueryUseCase  — 사업장 존재 확인 (외부 모듈 전용)
```

> Inbound Port는 호출자가 실제로 필요한 메서드만 노출합니다 (ISP).
> Repository 전체를 노출하지 않고, 외부 모듈이 필요한 기능만 정의합니다.

### Outbound Port (Secondary Port) — 모듈이 외부에 요구하는 계약

이 모듈이 외부 기술(DB, Redis 등)에 "무엇을 해달라"고 요청하는 인터페이스입니다.
구현체는 항상 Infrastructure Adapter입니다.

**위치는 의존하는 계층 기준으로 결정됩니다:**

| 위치 | 의존 계층 | 예시 |
|------|----------|------|
| `domain/port/` | Domain → Infrastructure | `UserRepository`, `CompanyRepository`, `PasswordEncryptor`, `TokenIssuer`, `Authenticator` |
| `application/port/` | Application → Infrastructure | `RefreshTokenStore` |

---

## 모듈 내 패키지 구조

```
{feature}/
├── presentation/
│   ├── {Feature}Controller.java
│   ├── request/             # 요청 DTO
│   ├── response/            # 응답 DTO
│   └── mapper/              # Request/Response ↔ Command (MapStruct)
├── application/
│   ├── {Feature}Service.java
│   ├── command/             # Command 객체, Result VO (Java Record)
│   └── port/                # Application Port 인터페이스
├── domain/
│   ├── {Entity}.java        # 도메인 모델
│   └── port/                # Domain Port 인터페이스
└── infrastructure/
    ├── Jpa{Entity}Entity.java
    ├── Jpa{Entity}Repository.java
    ├── adapter/             # Port 구현체
    └── mapper/              # JPA 엔티티 ↔ 도메인 모델 (MapStruct)
```

---

## 데이터 흐름

### 쓰기 흐름 (Command)

```
HTTP Request
    ↓
Controller          — Request DTO 수신
    ↓ (PresentationMapper)
Command             — 불변 입력 객체 (Java Record)
    ↓
Application Service — 유스케이스 조율, 트랜잭션 관리
    ↓ (Domain Port)
Domain Model        — 비즈니스 규칙 실행 (정적 팩토리 메서드)
    ↓ (DomainEntityMapper)
JPA Entity / 외부 기술
```

### 읽기 흐름 (Query)

```
HTTP Request
    ↓
Controller
    ↓
Application Service — Domain Port로 조회
    ↓ (DomainEntityMapper)
Domain Model
    ↓ (PresentationMapper)
Response DTO
    ↓
ApiResponse<T>      — HTTP Response
```

---

## 객체 변환 경계

| 변환 위치 | 변환 방향 | 담당 매퍼 |
|---|---|---|
| Presentation ↔ Application | Request ↔ Command, Domain ↔ Response | `{Domain}PresentationMapper` |
| Domain ↔ Infrastructure | Domain Model ↔ JPA Entity | `{Domain}DomainEntityMapper` |

컨트롤러, 서비스에서 직접 변환하지 않습니다.

---

## 의존성 방향

```
Presentation ──→ Application ──→ Domain
                                   ↑
                 Infrastructure ───┘   (의존성 역전)
```

Infrastructure는 Domain Port를 구현함으로써 Domain을 향해 의존합니다.  
Domain은 어떤 계층도 참조하지 않습니다.

---

## 모듈 간 통신 (Cross-Module Communication)

### 핵심 규칙

다른 모듈의 기능이 필요할 때, 반드시 해당 모듈의 **Inbound Port (application/port/)** 를 통해 접근합니다.
Domain Port (domain/port/) 를 외부 모듈에서 직접 참조하는 것은 금지입니다.

**이유:**
- `domain/port/Repository`는 해당 모듈이 인프라에 요구하는 **Outbound 계약**입니다.
  외부에 공개하는 인터페이스가 아니며, 참조 시 모듈 내부 구현 세부사항이 노출됩니다.
- `application/port/UseCase`는 해당 모듈이 외부에 공개한 **Inbound 계약**입니다.
  내부 구현이 바뀌어도 이 계약이 유지되는 한 호출자는 영향을 받지 않습니다.

### 올바른 방향 (O)

```
[contract 모듈]
  ContractService
      │
      ▼ (application/port/ — Inbound Port)
  WorkplaceQueryUseCase         ← client_management 모듈이 외부에 공개한 계약
      │
      ▼ (구현체)
  WorkplaceQueryService
      │
      ▼ (domain/port/ — Outbound Port, 모듈 내부)
  WorkplaceRepository
```

### 잘못된 방향 (X)

```
[contract 모듈]
  ContractService
      │
      ▼ (domain/port/ — Outbound Port를 외부에서 직접 참조)
  WorkplaceRepository           ← 캡슐화 위반: 모듈 내부 계약 노출
```

### Inbound Port 설계 지침

- **좁은 인터페이스**: 호출자가 필요한 기능만 메서드로 정의합니다. Repository 전체를 노출하지 않습니다.
- **의미 있는 이름**: `WorkplaceQueryUseCase`, `WorkplaceCommandUseCase`처럼 역할을 명시합니다.
- **구현체는 Application Service**: `WorkplaceQueryService implements WorkplaceQueryUseCase`
