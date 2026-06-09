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
    CompanyRepository (domain/port) → CompanyRepositoryAdapter → CompanyJpaRepository

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
| Response DTO | `{대상}Response` | `CompanyResponse`, `WorkplaceListResponse` |
| JPA 엔티티 | `{도메인}Entity` | `CompanyEntity` |
| JPA Repository | `{도메인}JpaRepository` | `CompanyJpaRepository` |
| Repository Adapter | `{도메인}RepositoryAdapter` | `CompanyRepositoryAdapter` |
| 기타 Adapter | 기술+역할 | `BCryptPasswordEncryptor`, `JwtTokenIssuer` |
| Presentation 매퍼 | `{도메인}Mapper` | `CompanyMapper` |
| Infrastructure 매퍼 | `{도메인}EntityMapper` | `CompanyEntityMapper` |

> **Response DTO 주의**: 이름에 UI 컴포넌트(`Table`, `Grid`, `Card` 등)를 포함하지 않습니다.
> 동일한 응답이 다양한 UI로 렌더링될 수 있으므로 용도가 아닌 도메인 개념으로 명명합니다.
> 예) `WorkplaceTableListResponse` (X) → `WorkplaceListResponse` (O)

#### 매퍼 메서드 명
- `toCreateCommand()` — 생성 Request → Create Command
- `toUpdateCommand()` — 수정 Request → Update Command
- `toResponse()` / `toResponses()` — Domain → Response DTO
- `toListResponse()` / `toListResponses()` — VO(목록 아이템) → List Response DTO
- `toEntity()` — Domain → JPA 엔티티
- `toDomain()` / `toDomainList()` — JPA 엔티티 → Domain

> **Command 매퍼 규칙**: 동일 도메인에 생성/수정 Command가 공존할 경우 `toCommand()` 대신 동작을 명시합니다.
> 단일 Command만 존재하는 도메인은 `toCreateCommand()` 형태로 작성합니다.

#### Repository Port 메서드 명
| 동작 | 메서드 패턴 | 반환 타입 | 예시 |
|------|------------|---------|------|
| 저장 | `save(T entity)` | `T` | `save(Company company)` |
| PK 단건 조회 | `findById(Long id)` | `T` | `findById(Long id)` |
| 필드 조건 단건 조회 | `findBy{Field}(value)` | `T` | `findByUsername(String username)` |
| 부모 ID 기준 목록 조회 | `findBy{ParentId}(Long id)` | `List<VO>` | `findByCompanyId(Long companyId)` |
| 전체 목록 조회 | `findAll()` | `List<T>` | `findAll()` |
| 존재 확인 | `existsBy{Field}(value)` | `boolean` | `existsByUsername(String username)` |
| 삭제 | `deleteById(Long id)` | `void` | `deleteById(Long id)` |

> **단건 조회 반환 타입 규칙**: Domain Port의 `findById` / `findBy{Field}` 는 `Optional` 을 반환하지 않습니다.
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
- **Command / VO**: 불변 값 객체는 Java **Record**로 작성합니다.
  - Command: `application/command/` 패키지
  - VO (결과값): `application/command/` 패키지
    - 순수 도메인 개념의 값 객체만 `domain/` 패키지 사용
    - Application 서비스가 반환하는 쿼리 결과 VO는 반드시 `application/command/`에 위치
    - `application/result/` 등 별도 패키지를 생성하지 않습니다

### 9. Swagger 어노테이션 규칙
- 모든 Controller 클래스에 `@Tag(name = "...", description = "...")` 어노테이션을 추가합니다.
- 엔드포인트별 `@Operation`, `@ApiResponse` 어노테이션은 선택 사항이며, 복잡한 API에만 추가합니다.

### 10. 패키지별 규칙
패키지별 세부 규칙은 각 모듈의 `CLAUDE.md`를 참고합니다.

### 11. ErrorCode 컨벤션

#### 분류
| 분류 | 네이밍 패턴 | 예시 |
|------|------------|------|
| 범용 HTTP 상태 | `{CONDITION}` | `NOT_FOUND`, `CONFLICT`, `BAD_REQUEST` |
| 도메인 특화 비즈니스 규칙 | `{DOMAIN}_{CONDITION}` | `SCHEDULE_ALREADY_EXISTS` |

- 두 분류 모두 `global/exception/ErrorCode.java` 단일 enum에서 관리합니다.
- 도메인 특화 코드는 반드시 도메인 prefix를 붙입니다.
- 인라인 메시지 override가 필요한 경우 → 먼저 도메인 특화 ErrorCode(`{DOMAIN}_{CONDITION}`) 추가 여부를 검토합니다.
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