# 프로젝트 가이드라인 (CLAUDE.md)
이 파일은 Claude Code(claude.ai/code)가 이 저장소에서 작업할 때 따라야 하는 **전역 개발 규칙**을 정의합니다.

## 프로젝트 원칙
- **헥사고날 아키텍처(Ports & Adapters) + Domain Driven Design**를 기반으로 기능 모듈 단위로 구성됩니다.
- 모든 응답은 **한국어**로 작성합니다.
- 아키텍처 상세내용은 `ARCHITECTURE.md`를 참고합니다.
- 앱 실행 후 API 문서는 `/swagger-ui.html`에서 확인할 수 있습니다.

## 기술 스택
- **Java** 21
- **Spring Boot** 3.5.x
- **Spring Security** + JWT (JJWT 0.12.x)
- **Spring Data JPA** + MySQL
- **Spring Data Redis** (Refresh Token 저장소)
- **MapStruct** 1.6.x
- **Lombok**
- **SpringDoc OpenAPI** 2.8.x (Swagger UI: `/swagger-ui.html`)

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
- 
### 2. 신규 기능 추가 시
기능은 반드시 **기능 모듈 단위**로 추가합니다.

    Ex)
    - auth
    - client_management
    - schedule

- global에 기능성 코드를 추가하지 않습니다.
- global은 공통 인프라만 관리합니다.

### 3. Mapping 규칙
- 객체 변환은 `MapStruct`를 사용합니다.
- MapStruct 매퍼는 `presentation/mapper/`(요청/응답 ↔ 커맨드)와 `infrastructure/mapper/`(JPA 엔티티 ↔ 도메인)에 위치합니다. 컨트롤러나 서비스에서 직접 매핑하지 않습니다.

### 4. Repository 규칙
Application Service는 Spring Data Repository를 직접 사용하지 않습니다.
반드시 `domain/port`를 통해 접근합니다.

    Ex)
    CompanyRepository (domain/port) → JpaCompanyRepositoryAdapter → JpaCompanyRepository

### 5. 생성자 주입
Lombok `@RequiredArgsConstructor`를 통한 생성자 주입만 사용합니다. 필드 주입은 사용하지 않습니다.

### 6. 응답 규칙
모든 엔드포인트는 `global/web/`의 `ApiResponse<T>`를 반환합니다.

### 7. 네이밍 컨벤션

#### 클래스 명
| 구분 | 패턴 | 예시 |
|------|------|------|
| Controller | `{도메인}Controller` | `CompanyController` |
| Service | `{도메인}Service` | `CompanyService` |
| Domain 모델 | 단순 명사 | `Company`, `User` |
| Domain Port | 역할 기반 명사 | `CompanyRepository`, `TokenIssuer` |
| Application Port | 역할 기반 명사 | `RefreshTokenStore` |
| Command | `{동작}{대상}Command` (Record) | `CreateCompanyCommand` |
| VO (결과값) | 의미 있는 명사 (Record) | `TokenResult`, `AuthenticatedUser` |
| Request DTO | `{동작}{대상}Request` | `CreateCompanyRequest` |
| Response DTO | `{대상}Response` | `CompanyResponse` |
| JPA 엔티티 | `Jpa{도메인}Entity` | `JpaCompanyEntity` |
| JPA Repository | `Jpa{도메인}Repository` | `JpaCompanyRepository` |
| Repository Adapter | `{도메인}RepositoryAdapter` | `CompanyRepositoryAdapter` |
| 기타 Adapter | 기술+역할 | `BCryptPasswordEncryptor`, `JwtTokenIssuer` |
| Presentation 매퍼 | `{도메인}PresentationMapper` | `CompanyPresentationMapper` |
| Infrastructure 매퍼 | `{도메인}DomainEntityMapper` | `CompanyDomainEntityMapper` |

#### 매퍼 메서드 명
- `toCommand()` — Request → Command
- `toResponse()` / `toResponses()` — Domain → Response DTO
- `toEntity()` — Domain → JPA 엔티티
- `toDomain()` / `toDomainList()` — JPA 엔티티 → Domain

### 8. 도메인 모델 패턴
- **Lombok 조합**: `@Builder(toBuilder = true)` + `@Getter` + `@AllArgsConstructor` + `@NoArgsConstructor`
- **생성 로직**: 생성자 직접 사용 금지. 정적 팩토리 메서드로 비즈니스 의미를 부여합니다.
- **Command / VO**: 불변 값 객체는 Java **Record**로 작성합니다.
  - Command: `application/command/` 패키지
  - VO (결과값): `domain/` 또는 `application/command/` 패키지

### 9. Swagger 어노테이션 규칙
- 모든 Controller 클래스에 `@Tag(name = "...", description = "...")` 어노테이션을 추가합니다.
- 엔드포인트별 `@Operation`, `@ApiResponse` 어노테이션은 선택 사항이며, 복잡한 API에만 추가합니다.

### 10. 패키지별 규칙
패키지별 세부 규칙은 각 모듈의 `CLAUDE.md`를 참고합니다.