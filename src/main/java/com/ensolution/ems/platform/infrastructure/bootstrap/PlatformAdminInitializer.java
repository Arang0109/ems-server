package com.ensolution.ems.platform.infrastructure.bootstrap;

import com.ensolution.ems.auth.application.port.in.CreateUserCommand;
import com.ensolution.ems.auth.application.port.in.RoleCommandUseCase;
import com.ensolution.ems.auth.application.port.in.UserCommandUseCase;
import com.ensolution.ems.auth.application.port.in.UserQueryUseCase;
import com.ensolution.ems.platform.application.port.out.TenantRepository;
import com.ensolution.ems.platform.domain.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 서버 최초 배포 시 운영자(PLATFORM_ADMIN) 계정을 멱등하게 확보하는 부트스트랩.
 * 기동 시 표준 역할 → 시스템 테넌트 → 운영자 User 순으로 존재 확인 후 없을 때만 생성한다.
 * (CustomUserDetailsService가 로그인 시 tenantId로 TenantEntity를 조회하므로 테넌트가 User보다 먼저 생성되어야 한다.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformAdminInitializer implements ApplicationRunner {

	private static final String PLATFORM_ADMIN_ROLE = "PLATFORM_ADMIN";

	/** 표준 역할 시드. 빈 DB에서도 부팅 가능하도록 부트스트랩이 멱등 확보한다. */
	private static final Map<String, String> STANDARD_ROLES = Map.of(
		PLATFORM_ADMIN_ROLE, "플랫폼 운영자",
		"ADMIN", "관리자",
		"LAB", "실험실",
		"FIELD", "측정팀",
		"DOC", "문서",
		"USER", "일반 사용자"
	);

	private final PlatformBootstrapProperties properties;
	private final RoleCommandUseCase roleCommandUseCase;
	private final TenantRepository tenantRepository;
	private final UserQueryUseCase userQueryUseCase;
	private final UserCommandUseCase userCommandUseCase;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!properties.isEnabled()) {
			log.info("[bootstrap] platform.bootstrap.enabled=false — 운영자 부트스트랩을 건너뜁니다.");
			return;
		}
		if (!StringUtils.hasText(properties.getUsername()) || !StringUtils.hasText(properties.getPassword())) {
			log.warn("[bootstrap] 운영자 username/password가 설정되지 않아 부트스트랩을 건너뜁니다. "
				+ "(PLATFORM_ADMIN_USERNAME / PLATFORM_ADMIN_PASSWORD 환경변수 확인)");
			return;
		}

		// 1. 표준 역할 확보 (멱등)
		STANDARD_ROLES.forEach((name, description) -> {
			roleCommandUseCase.ensureRole(name, description);
		});
		Long platformAdminRoleId = roleCommandUseCase.ensureRole(PLATFORM_ADMIN_ROLE, STANDARD_ROLES.get(PLATFORM_ADMIN_ROLE));

		// 2. 시스템 테넌트 확보 (멱등)
		Long systemTenantId = ensureSystemTenant();

		// 3. 운영자 User 확보 (멱등)
		String username = properties.getUsername();
		if (userQueryUseCase.existsByUsername(username)) {
			log.info("[bootstrap] 운영자 계정('{}')이 이미 존재하여 생성을 건너뜁니다.", username);
			return;
		}
		userCommandUseCase.createUser(new CreateUserCommand(
			systemTenantId,
			platformAdminRoleId,
			username,
			properties.getPassword(),
			properties.getName(),
			properties.getDepartment(),
			properties.getEmail(),
			properties.getTel()
		));
		log.info("[bootstrap] 운영자 계정('{}')을 생성했습니다. 최초 로그인 후 비밀번호를 변경하세요.", username);
	}

	private Long ensureSystemTenant() {
		String bizNumber = properties.getSystemTenantBizNumber();
		return tenantRepository.findByBizNumber(bizNumber)
			.map(Tenant::getId)
			.orElseGet(() -> {
				Tenant created = tenantRepository.save(
					Tenant.system(properties.getSystemTenantName(), bizNumber)
				);
				log.info("[bootstrap] 시스템 테넌트('{}')를 생성했습니다.", properties.getSystemTenantName());
				return created.getId();
			});
	}
}
