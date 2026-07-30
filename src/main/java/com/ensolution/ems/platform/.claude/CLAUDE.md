# platform 모듈 가이드라인

플랫폼 운영자(**PLATFORM_ADMIN**) 전용 관심사를 담는 모듈. 자사 서비스 운영자가 고객사(테넌트)의
생명주기(발급·조회)를 관리하고, 서버 최초 배포 시 운영자 계정을 부트스트랩한다.
테넌트 내부 업무(사업장·측정시설 등)를 다루는 `tenant` 모듈과 **관심사가 분리**된다.

## 경계 규칙

- **역할 층위**: `PLATFORM_ADMIN`은 특정 테넌트에 속하지 않는 전역 역할이며, 계층상 `ROLE_ADMIN`의 상위다
  (`ROLE_PLATFORM_ADMIN > ROLE_ADMIN`). 테넌트 관리자(`ADMIN`)는 `/api/platform/**`에 접근할 수 없다(403).
- **경로 보호**: `/api/platform/**`는 `SecurityConfig`에서 `hasRole("PLATFORM_ADMIN")`으로 보호한다.
  이 모듈의 API는 테넌트 소유권 격리(principal.getTenantId())를 쓰지 않는다 — 운영자는 전 테넌트를 조회한다.
- **TenantEntity는 이 모듈이 소유하지 않는다**: JPA 영속 앵커(`TenantEntity`/`TenantJpaRepository`)는
  멀티테넌시 공용이라 `tenant/infrastructure`에 그대로 둔다. 본 모듈의 `TenantRepositoryAdapter`가 이를 재사용한다.
  platform은 도메인 `Tenant`(생명주기 모델)만 소유한다.
- **인터모듈 연동은 auth의 인바운드 포트로만**: 초기 관리자·운영자 계정 생성은 auth의
  `UserCommandUseCase`, `RoleQueryUseCase`, `RoleCommandUseCase`(`auth/application/port/in`)를 통해서만 한다.
  auth의 domain/infra를 직접 참조하지 않는다.
- **부트스트랩은 계층 규칙의 명시적 예외**: `PlatformAdminInitializer`는 infrastructure에 있으면서
  `@Transactional` + 아웃바운드 포트(`TenantRepository`) + 타 모듈 인바운드 포트를 직접 조합해 절차를 오케스트레이션한다.
  일반 규칙대로면 application 유스케이스로 올려야 하지만, **런타임 유스케이스가 아니라 배포 1회성 설치 코드**이므로
  현 위치를 유지한다. 별도 inbound port·Command를 만들어 얇은 러너로 쪼개는 리팩터링은 하지 않는다.
  - 예외 범위는 이 클래스에 한정한다. 부트스트랩 이외의 로직(운영 중 호출되는 흐름)을 여기에 추가하지 않는다.
    부트스트랩 절차가 커지거나 API로 재사용해야 하면 그때 application으로 승격한다.

## 구성

- `domain/Tenant` — 고객사 도메인 모델. 팩토리 `provision(...)`(발급, ACTIVE), `system(...)`(전용 시스템 테넌트, INTERNAL).
- `application/service/PlatformService` — `provisionTenant`(테넌트+초기 ADMIN 원자 생성), `getTenant`, `getTenantList`.
  `TenantQueryUseCase`를 구현한다. **클래스명을 `TenantService`로 두지 않는다** — tenant 모듈과 빈 이름(`tenantService`)이 충돌한다.
- `application/port/in/TenantQueryUseCase`·`TenantSummary` — 타 모듈 공개 계약. schedule이 측정 시점
  고객사 스냅샷을 조립할 때 `getTenantSummary(tenantId)`로 조회한다. 변환은 `application/mapper/TenantSummaryMapper`.
- `application/port/out/TenantRepository` — 아웃바운드 포트. `findById`는 없으면 `TENANT_NOT_FOUND`.
- `infrastructure/adapter/TenantRepositoryAdapter` — `TenantJpaRepository` 재사용.
- `infrastructure/bootstrap/PlatformAdminInitializer` — 멱등 `ApplicationRunner`. 표준 역할 → 시스템 테넌트 → 운영자 순 확보.
  설정은 `platform.bootstrap.*`(환경변수 주입). `enabled=false`거나 username/password 미설정 시 no-op.
  계층 예외는 위 경계 규칙 참고.
- `presentation/controller/PlatformTenantController` — `/api/platform/tenants` (POST 발급 / GET 목록 / GET 단건).
