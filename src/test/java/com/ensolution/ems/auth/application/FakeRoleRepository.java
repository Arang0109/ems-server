package com.ensolution.ems.auth.application;

import com.ensolution.ems.auth.domain.Role;
import com.ensolution.ems.auth.application.port.out.RoleRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 역할 부여 제한 검증용 인메모리 {@link RoleRepository}.
 * <p>
 * 역할은 전역 마스터라 tenant 파라미터가 없다. 실제 어댑터와 동일하게 미존재 조회는
 * {@code ROLE_NOT_FOUND}를 던진다 — 이 규약이 흔들리면 "존재하지 않는 역할"과
 * "부여할 수 없는 역할"의 응답이 뒤섞인다.
 */
public class FakeRoleRepository implements RoleRepository {

	private final List<Role> roles = new ArrayList<>();

	public void given(Long roleId, String name) {
		roles.add(Role.builder().roleId(roleId).name(name).build());
	}

	@Override
	public Role findById(Long id) {
		return roles.stream()
			.filter(role -> Objects.equals(id, role.getRoleId()))
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorCode.ROLE_NOT_FOUND));
	}

	@Override
	public Role findByName(String name) {
		return roles.stream()
			.filter(role -> Objects.equals(name, role.getName()))
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorCode.ROLE_NOT_FOUND));
	}

	@Override
	public List<Role> findAll() {
		return List.copyOf(roles);
	}

	@Override
	public boolean existsByName(String name) {
		return roles.stream().anyMatch(role -> Objects.equals(name, role.getName()));
	}

	@Override
	public Role save(Role role) {
		throw new UnsupportedOperationException();
	}
}
