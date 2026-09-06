package com.ensolution.ems.auth.application.service;

import com.ensolution.ems.auth.application.port.in.UserCredentialQueryUseCase;
import com.ensolution.ems.auth.application.port.in.UserCredentialSummary;
import com.ensolution.ems.auth.application.port.in.UserQueryUseCase;
import com.ensolution.ems.auth.application.port.in.UserSummary;
import com.ensolution.ems.auth.domain.Role;
import com.ensolution.ems.auth.domain.User;
import com.ensolution.ems.auth.application.port.out.RoleRepository;
import com.ensolution.ems.auth.application.port.out.UserRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService implements UserQueryUseCase, UserCredentialQueryUseCase {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;

	@Override
	public UserSummary getUser(Long userId, Long tenantId) {
		User user = userRepository.findById(userId, tenantId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		return toSummary(user, roleNameOf(user.getRoleId()));
	}

	/**
	 * 역할 이름을 한 번에 읽어 메모리에서 붙인다.
	 * <p>
	 * 사용자마다 {@code roleRepository.findById}를 부르면 목록 크기만큼 조회가 나간다(N+1).
	 * 역할은 전 tenant가 공유하는 전역 마스터라 건수가 적고, 한 번 읽어 두면 목록이 커져도 조회는 2회다.
	 */
	@Override
	public List<UserSummary> getUserList(Long tenantId) {
		Map<Long, String> roleNameById = roleRepository.findAll().stream()
			.collect(Collectors.toMap(Role::getRoleId, Role::getName));

		return userRepository.findAll(tenantId).stream()
			.map(user -> toSummary(user, roleNameById.get(user.getRoleId())))
			.toList();
	}

	@Override
	public boolean existsByUsername(String username) {
		return userRepository.existsByUsername(username);
	}

	@Override
	public Optional<UserCredentialSummary> findCredentialByUsername(String username) {
		return userRepository.findByUsername(username)
			.map(user -> new UserCredentialSummary(
				user.getId(),
				user.getTenantId(),
				user.getUsername(),
				user.getPassword(),
				user.getName(),
				roleNameOf(user.getRoleId())
			));
	}

	private String roleNameOf(Long roleId) {
		return roleRepository.findById(roleId).getName();
	}

	private UserSummary toSummary(User user, String roleName) {
		return new UserSummary(
			user.getId(),
			user.getTenantId(),
			user.getUsername(),
			user.getName(),
			user.getRoleId(),
			roleName,
			user.getDepartment(),
			user.getEmail(),
			user.getTel()
		);
	}
}
