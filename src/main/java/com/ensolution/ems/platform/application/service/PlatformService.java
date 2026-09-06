package com.ensolution.ems.platform.application.service;

import com.ensolution.ems.auth.application.port.in.CreateUserCommand;
import com.ensolution.ems.auth.application.port.in.RoleQueryUseCase;
import com.ensolution.ems.auth.application.port.in.UserCommandUseCase;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.platform.application.command.ProvisionTenantCommand;
import com.ensolution.ems.platform.application.command.TenantAdminCommand;
import com.ensolution.ems.platform.application.command.TenantListItem;
import com.ensolution.ems.platform.application.port.out.TenantRepository;
import com.ensolution.ems.platform.domain.Tenant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 플랫폼 운영자(PLATFORM_ADMIN) 전용 고객사(테넌트) 유스케이스.
 * 초기 관리자 계정 생성은 auth의 인바운드 포트(UserCommandUseCase/RoleQueryUseCase)를 통해서만 수행한다.
 * <p>
 * <b>타 모듈 공개 계약({@code TenantQueryUseCase})은 여기서 구현하지 않는다.</b>
 * 이 서비스는 auth에 의존하는데, auth의 인증 경로가 다시 테넌트 요약을 필요로 해서
 * 한 서비스가 둘을 겸하면 빈 순환이 된다. 그 구현은 {@link TenantQueryService}에 있다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PlatformService {

	private static final String ADMIN_ROLE_NAME = "ADMIN";

	private final TenantRepository tenantRepository;
	private final RoleQueryUseCase roleQueryUseCase;
	private final UserCommandUseCase userCommandUseCase;

	/**
	 * 고객사를 발급하고 초기 관리자(ADMIN)를 함께 생성한다. 하나의 트랜잭션으로 원자 처리된다.
	 */
	public Tenant provisionTenant(ProvisionTenantCommand command) {
		if (tenantRepository.existsByBizNumber(command.bizNumber())) {
			throw new CustomException(ErrorCode.TENANT_ALREADY_EXISTS);
		}

		Tenant tenant = tenantRepository.save(
			Tenant.provision(command.name(), command.bizNumber(), command.subscriptionPlan())
		);

		Long adminRoleId = roleQueryUseCase.findRoleIdByName(ADMIN_ROLE_NAME);
		TenantAdminCommand admin = command.admin();
		userCommandUseCase.createUser(new CreateUserCommand(
			tenant.getId(),
			adminRoleId,
			admin.username(),
			admin.password(),
			admin.name(),
			admin.department(),
			admin.email(),
			admin.tel()
		));

		return tenant;
	}

	@Transactional(readOnly = true)
	public Tenant getTenant(Long tenantId) {
		return tenantRepository.findById(tenantId);
	}

	@Transactional(readOnly = true)
	public List<TenantListItem> getTenantList() {
		return tenantRepository.findAll();
	}
}
