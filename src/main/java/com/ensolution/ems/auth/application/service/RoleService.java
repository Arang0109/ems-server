package com.ensolution.ems.auth.application.service;

import com.ensolution.ems.auth.application.port.in.RoleCommandUseCase;
import com.ensolution.ems.auth.application.port.in.RoleQueryUseCase;
import com.ensolution.ems.auth.domain.Role;
import com.ensolution.ems.auth.application.port.out.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleService implements RoleQueryUseCase, RoleCommandUseCase {

	private final RoleRepository roleRepository;

	@Override
	@Transactional(readOnly = true)
	public Long findRoleIdByName(String name) {
		return roleRepository.findByName(name).getRoleId();
	}

	@Override
	public Long ensureRole(String name, String description) {
		if (roleRepository.existsByName(name)) {
			return roleRepository.findByName(name).getRoleId();
		}
		Role saved = roleRepository.save(
			Role.builder().name(name).description(description).build()
		);
		return saved.getRoleId();
	}

	/**
	 * 테넌트 범위 API로 <b>부여할 수 있는</b> 역할만 반환한다.
	 * <p>
	 * {@code PLATFORM_ADMIN}은 {@code UserValidator.requireAssignableRole}이 부여를 거부하는 역할이다.
	 * 고를 수 없는 것을 선택지로 내려보내면 클라이언트가 같은 규칙을 다시 구현해야 하고,
	 * 그러지 않으면 사용자가 고른 뒤 403을 받는다. 판정은 도메인({@link Role#isPlatformAdmin()})이 소유하며
	 * 이 목록과 부여 검증이 같은 근거를 본다.
	 */
	@Transactional(readOnly = true)
	public List<Role> getAssignableRoles() {
		return roleRepository.findAll().stream()
			.filter(role -> !role.isPlatformAdmin())
			.toList();
	}
}
