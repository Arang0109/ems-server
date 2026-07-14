package com.ensolution.ems.auth.application.service;

import com.ensolution.ems.auth.application.port.in.UserQueryUseCase;
import com.ensolution.ems.auth.application.port.in.UserSummary;
import com.ensolution.ems.auth.domain.Role;
import com.ensolution.ems.auth.domain.User;
import com.ensolution.ems.auth.domain.port.RoleRepository;
import com.ensolution.ems.auth.domain.port.UserRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService implements UserQueryUseCase {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;

	@Override
	public UserSummary getUser(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		return toSummary(user);
	}

	@Override
	public List<UserSummary> getUserList(Long tenantId) {
		return userRepository.findAll(tenantId).stream()
			.map(this::toSummary)
			.toList();
	}

	private UserSummary toSummary(User user) {
		Role role = roleRepository.findById(user.getRoleId());
		return new UserSummary(
			user.getId(),
			user.getTenantId(),
			user.getUsername(),
			user.getName(),
			user.getRoleId(),
			role.getName(),
			user.getDepartment(),
			user.getEmail(),
			user.getTel()
		);
	}
}
