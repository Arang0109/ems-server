package com.ensolution.ems.auth.application.service;

import com.ensolution.ems.auth.application.command.SignInCommand;
import com.ensolution.ems.auth.application.command.SignInResult;
import com.ensolution.ems.auth.application.command.SignUpCommand;
import com.ensolution.ems.auth.application.port.in.UserCommandUseCase;
import com.ensolution.ems.auth.application.port.in.CreateUserCommand;
import com.ensolution.ems.auth.application.port.in.UpdateUserCommand;
import com.ensolution.ems.auth.domain.Role;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.auth.domain.port.Authenticator;
import com.ensolution.ems.auth.domain.port.PasswordEncryptor;
import com.ensolution.ems.auth.domain.port.RoleRepository;
import com.ensolution.ems.auth.domain.port.TokenIssuer;
import com.ensolution.ems.auth.domain.AuthenticatedUser;
import com.ensolution.ems.auth.domain.TokenResult;
import com.ensolution.ems.auth.domain.User;
import com.ensolution.ems.auth.domain.port.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService implements UserCommandUseCase {
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncryptor passwordEncryptor;
	private final Authenticator authenticator;
	private final TokenIssuer tokenIssuer;

	public void createUser(CreateUserCommand command) {
		register(
			command.tenantId(),
			command.roleId(),
			command.username(),
			command.password(),
			command.name(),
			command.department(),
			command.email(),
			command.tel()
		);
	}

	public void updateUser(UpdateUserCommand command) {
		User user = userRepository.findById(command.userId())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		// 존재하지 않는 roleId면 ROLE_NOT_FOUND 예외
		if (command.roleId() != null) {
			roleRepository.findById(command.roleId());
		}

		User updated = user.update(
			command.roleId(),
			command.name(),
			command.department(),
			command.email(),
			command.tel()
		);

		userRepository.save(updated);
	}

	public void deleteUser(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		userRepository.deleteById(user.getId());
	}

	public void signUp(SignUpCommand command) {
		register(
			command.tenantId(),
			command.roleId(),
			command.username(),
			command.password(),
			command.name(),
			command.department(),
			command.email(),
			command.tel()
		);
	}

	private void register(
		Long tenantId,
		Long roleId,
		String username,
		String rawPassword,
		String name,
		String department,
		String email,
		String tel
	) {
		if (userRepository.existsByUsername(username)) {
			throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
		}

		// 존재하지 않는 roleId면 ROLE_NOT_FOUND 예외
		roleRepository.findById(roleId);

		String encodedPassword = passwordEncryptor.encode(rawPassword);

		User user = User.signUp(
			tenantId,
			roleId,
			username,
			encodedPassword,
			name,
			department,
			email,
			tel
		);

		userRepository.save(user);
	}
	
	public SignInResult signIn(SignInCommand command) {
		AuthenticatedUser authentication = authenticator.authenticate(
				command.username(), command.password()
		);
		
		TokenResult tokenResult = tokenIssuer.issue(authentication);

		return new SignInResult(
				tokenResult.accessToken(),
				tokenResult.refreshToken(),
				tokenResult.tenantId(),
				tokenResult.tenant(),
				tokenResult.username(),
				tokenResult.name(),
				tokenResult.role(),
				tokenResult.refreshTokenValidity()
		);
	}
	
	public List<Role> getRoleList() {
		return roleRepository.findAll();
	}
}
