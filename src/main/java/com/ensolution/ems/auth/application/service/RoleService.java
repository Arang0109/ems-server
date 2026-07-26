package com.ensolution.ems.auth.application.service;

import com.ensolution.ems.auth.application.port.in.RoleCommandUseCase;
import com.ensolution.ems.auth.application.port.in.RoleQueryUseCase;
import com.ensolution.ems.auth.domain.Role;
import com.ensolution.ems.auth.domain.port.RoleRepository;
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

	@Transactional(readOnly = true)
	public List<Role> getRoleList() {
		return roleRepository.findAll();
	}
}
