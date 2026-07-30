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
- MapStruct 매퍼는 `presentation/**/mapper/`(요청/응답 ↔ 커맨드)와 `infrastructure/mapper/`(JPA 엔티티 ↔ 도메인)에 위치합니다. 컨트롤러나 서비스에서 직접 매핑하지 않습니다.
  - 애그리거트가 여러 개인 모듈은 `presentation/{aggregate}/mapper/`로 애그리거트별 그룹핑합니다.
  - **인터모듈 매퍼**: 타 모듈의 inbound port DTO(`application/port/in`의 VO·Command) ↔ 자기 도메인/커맨드 변환은 `application/mapper/`에 둡니다. 소비 모듈이 공급 모듈의 port/in 계약과만 결합하도록 하는 경계 변환이며, 위 두 범주(요청·응답, JPA 엔티티)에 속하지 않습니다.
    - 예) `admin/application/mapper/MemberPortMapper` — auth의 `UserSummary`→admin `Member`, admin `CreateMemberCommand`→auth `UserCreateCommand`.

### 4. Repository 규칙
Application Service는 Spring Data Repository를 직접 사용하지 않습니다.
반드시 Outbound Port를 통해 접근하며, Outbound Port의 표준 위치는 `application/port/out/`입니다.

    Ex)
    ClientRepository (application/port/out) → ClientRepositoryAdapter → ClientJpaRepository

> **포트 위치 표준화**: Repository 등 Outbound Port는 `application/port/out/`, 외부 공개 Inbound Port(UseCase)는 `application/port/in/`에 둡니다.
> `client_management`는 이관 완료. `auth`·`contract`는 아직 `domain/port/`를 사용하며 향후 정렬 예정입니다.

### 5. 생성자 주입
Lombok `@RequiredArgsConstructor`를 통한 생성자 주입만 사용합니다. 필드 주입은 사용하지 않습니다.

### 6. 응답 규칙
모든 엔드포인트는 `global/web/`의 `ApiResponse<T>`를 반환합니다.

### 7. 네이밍 컨벤션

#### 클래스 명
| 구분 | 패턴 | 예시 |
|------|------|------|
| Controller | `{도메인}Controller` | `ClientController` |
| Service | `{도메인}Service` | `ClientService` |
| Detail Assembler | `{도메인}DetailAssembler` | `StackDetailAssembler` |
| Validator | `{애그리거트}Validator` | `StackValidator`, `TeamValidator` |
| Domain 모델 | 단순 명사 | `Client`, `User` |
| Outbound Port | 역할 기반 명사 | `ClientRepository`, `TokenIssuer` |
| Inbound Port (UseCase) | `{도메인}{동작}UseCase` | `WorkplaceQueryUseCase` |
| Command | `{동작}{대상}Command` (Record) | `CreateClientCommand`, `CreateUserCommand` |
| VO (결과값) | `{대상}{종류}` (Record, 아래 접미사 표 참고) | `SignInResult`, `WorkplaceListItem`, `StackDetail`, `UserSummary` |
| Request DTO | `{동작}{대상}Request` | `CreateClientRequest` |
| Response DTO | `{대상}Response` | `ClientResponse`, `WorkplaceListResponse` |
| JPA 엔티티 | `{도메인}Entity` | `ClientEntity` |
| JPA Repository | `{도메인}JpaRepository` | `ClientJpaRepository` |
| Repository Adapter | `{도메인}RepositoryAdapter` | `ClientRepositoryAdapter` |
| 기타 Adapter | 기술+역할 | `BCryptPasswordEncryptor`, `JwtTokenIssuer` |
| Presentation 매퍼 | `{도메인}Mapper` | `ClientMapper` |
| Infrastructure 매퍼 | `{도메인}EntityMapper` | `ClientEntityMapper` |

> **Response DTO 주의**: 이름에 UI 컴포넌트(`Table`, `Grid`, `Card` 등)를 포함하지 않습니다.
> 동일한 응답이 다양한 UI로 렌더링될 수 있으므로 용도가 아닌 도메인 개념으로 명명합니다.
> 예) `WorkplaceTableListResponse` (X) → `WorkplaceListResponse` (O)

> **Service 분리 방침**: 유스케이스는 도메인당 단일 `{도메인}Service`로 둡니다. Command/Query 서비스 분리(CQRS)는
> 현재 채택하지 않으며, 규모가 커지면 재검토합니다. 트리 조립처럼 별도 책임은 `{도메인}DetailAssembler`로 분리합니다.

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
| `~Summary` | application **출력**, **타 모듈 공개용** | `{대상}Summary` | `application/port/in/` | `UserSummary`, `ContractSummary` |

**프리픽스 2대 원칙**
- **입력 계열(Request/Command/Result)**: 동작을 앞에 — `Create`/`Update`/`SignIn` + 대상. (예: `CreateMemberCommand`)
- **조회 결과 VO(ListItem/Detail/Summary)**: 대상을 앞에 — 대상 + 종류접미사. (예: `WorkplaceListItem`)
- 타 모듈 공개 `port/in`의 **Command도 입력 계열이므로 동작을 앞에** 둡니다. (예: `CreateUserCommand` — `UserCreateCommand` (X))

> **`~Result` vs `~Response` 구분 유지**: `~Result`는 application VO, `~Response`는 presentation DTO입니다.
> 계층 경계를 나타내므로 (`SignInResult` → `SignInResponse`) 하나로 합치지 않습니다.

> **Command 하위 패키지**: 커맨드/VO 파일이 적으면 `application/command/` flat로 둡니다.
> 애그리거트가 많아 파일이 늘면 `command/create·update·detail·list_item/`처럼 종류별로 하위 그룹핑합니다 (tenant 모듈 참고).

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
| 동작 | 메서드 패턴                      | 반환 타입 | 예시 |
|------|-----------------------------|---------|------|
| 저장 | `save(T entity)`            | `T` | `save(Client client)` |
| PK 단건 조회 | `findById(Long id)`         | `T` | `findById(Long id)` |
| 필드 조건 단건 조회 | `findBy{Field}(value)`      | `T` | `findByUsername(String username)` |
| 부모 ID 기준 목록 조회 | `findBy{ParentId}(Long id)` | `List<VO>` | `findByClientId(Long clientId)` |
| 전체 목록 조회 | `findAll()`                 | `List<T>` | `findAll()` |
| 존재 확인 | `existsBy{Field}(value)`    | `boolean` | `existsByUsername(String username)` |
| 삭제 | `deleteById(Long id)`       | `void` | `deleteById(Long id)` |

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
- `@Component` + `@RequiredArgsConstructor`로 선언하며, **`application/port/out/`과 타 모듈 `application/port/in/`만 주입**받습니다. Spring Data Repository·JPA 엔티티를 직접 참조하지 않습니다.
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
> 레퍼런스 구현은 `tenant/application/validator/`입니다. 다른 모듈은 순차 적용 예정입니다.

### 11. 패키지별 규칙
패키지별 세부 규칙은 각 모듈의 `CLAUDE.md`를 참고합니다.

### 12. ErrorCode 컨벤션

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