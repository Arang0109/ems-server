# CLAUDE.md

이 파일은 Claude Code(claude.ai/code)가 이 저장소에서 작업할 때 참고하는 가이드입니다.

## 빌드 및 실행 명령어

```bash
# 빌드
./gradlew build           # (Linux/Mac) 또는 gradlew.bat build (Windows)

# 실행
./gradlew bootRun

# 전체 테스트
./gradlew test

# 단일 테스트 클래스 실행
./gradlew test --tests "com.ensolution.ems.SomeTest"
```

앱 실행 후 API 문서는 `/swagger-ui.html` 에서 확인할 수 있습니다.

## 사전 요구사항

- Java 21
- MySQL: `localhost:3306`, 데이터베이스 `ems-db` (user: `root`, pass: `0000`)
- Redis: `localhost:6379`
- 프로젝트 루트에 `.env` 파일 (필수 값: `JWT_SECRET`, `AT_VALID`, `RT_VALID`, `SPRING_PROFILES_ACTIVE=local`)

## 아키텍처

**헥사고날 아키텍처(Ports & Adapters)** + DDD를 기반으로 기능 모듈 단위로 구성됩니다.

```
src/main/java/com/ensolution/ems/
├── auth/                      # 인증 및 JWT
├── client_management/         # 업체 / 사업장 / 굴뚝 관리
└── global/                    # 공통: 보안 설정, Swagger, 공통 enum, ApiResponse
```

각 기능 모듈은 4개의 레이어로 구성됩니다.

| 레이어 | 패키지 | 역할 |
|---|---|---|
| Presentation | `presentation/` | REST 컨트롤러, 요청/응답 DTO, MapStruct 매퍼 |
| Application | `application/` | 유스케이스 서비스, 커맨드 객체 |
| Domain | `domain/` | 도메인 모델 (프레임워크 의존성 없음), 포트 인터페이스 |
| Infrastructure | `infrastructure/` | JPA 엔티티, Spring Data 레포지토리, 포트 어댑터 구현체 |

의존성 방향: `presentation → application → domain ← infrastructure`

## 주요 개발 규칙

**매핑:** MapStruct 매퍼는 `presentation/mapper/`(요청/응답 ↔ 커맨드)와 `infrastructure/mapper/`(JPA 엔티티 ↔ 도메인)에 위치합니다. 컨트롤러나 서비스에서 직접 매핑하지 않습니다.

**API 응답:** 모든 엔드포인트는 `global/web/`의 `ApiResponse<T>`를 반환합니다.

**포트:** `domain/port/`의 도메인 인터페이스는 `infrastructure/adapter/`의 어댑터가 구현합니다. 서비스는 포트 인터페이스에만 의존하며, 인프라 클래스를 직접 참조하지 않습니다.

**의존성 주입:** Lombok `@RequiredArgsConstructor`를 통한 생성자 주입만 사용합니다. 필드 주입은 사용하지 않습니다.

## 보안

JWT 액세스 토큰(HttpOnly 쿠키) + Redis에 캐시된 리프레시 토큰 방식입니다. 역할 계층:

```
ROLE_ADMIN → ROLE_LAB, ROLE_FIELD, ROLE_DOC → ROLE_USER
```

보안 설정은 `global/security/config/SecurityConfig`에 있습니다. 공개 경로: `/api/auth/sign-up`, `/api/auth/sign-in`, Swagger 문서. 관리자 전용: `/api/admin/**`.

CORS 허용 출처: `localhost:5173`, `127.0.0.1:5173` (프론트엔드 개발), `54.180.112.112:3000` (운영 서버).

## 도메인 모델

`Company(업체)` → (1:N) → `Workplace(사업장)` → (1:N) → `Stack(굴뚝/배출원)`

`Stack`은 물리적 속성(높이, 형태, 방향, 등급 TYPE_1~TYPE_5)과 SEMS 번호(국가 환경 모니터링 시스템 ID)를 기록합니다.