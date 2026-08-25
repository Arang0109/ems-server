package com.ensolution.ems.auth.application.service;

import com.ensolution.ems.auth.application.command.SignInCommand;
import com.ensolution.ems.auth.application.command.SignInResult;
import com.ensolution.ems.auth.application.port.in.UserCommandUseCase;
import com.ensolution.ems.auth.application.port.in.CreateUserCommand;
import com.ensolution.ems.auth.application.port.in.UpdateUserCommand;
import com.ensolution.ems.client_management.application.port.in.TeamQueryUseCase;
import com.ensolution.ems.client_management.application.port.in.UserTeamSummary;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.auth.application.validator.UserValidator;
import com.ensolution.ems.auth.domain.port.Authenticator;
import com.ensolution.ems.auth.domain.port.PasswordEncryptor;
import com.ensolution.ems.auth.domain.port.TokenIssuer;
import com.ensolution.ems.auth.domain.AuthenticatedUser;
import com.ensolution.ems.auth.domain.TokenResult;
import com.ensolution.ems.auth.domain.User;
import com.ensolution.ems.auth.domain.port.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService implements UserCommandUseCase {
	private final UserRepository userRepository;
	private final PasswordEncryptor passwordEncryptor;
	private final Authenticator authenticator;
	private final TokenIssuer tokenIssuer;
	private final TeamQueryUseCase teamQueryUseCase;
	private final UserValidator userValidator;

	public void createUser(CreateUserCommand command) {
		userValidator.requireAssignableRole(command.roleId());
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

	/**
	 * 부트스트랩 전용 운영자 계정 생성. 역할 부여 제한을 적용하지 않는 유일한 경로다.
	 * roleId는 부트스트랩이 {@code ensureRole}로 직접 확보한 값이라 존재가 보장된다.
	 */
	public void createPlatformAdmin(CreateUserCommand command) {
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
		User user = userRepository.findById(command.userId(), command.tenantId())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		// 존재하지 않는 역할이면 ROLE_NOT_FOUND, PLATFORM_ADMIN이면 ROLE_NOT_ASSIGNABLE
		userValidator.requireAssignableRole(command.roleId());

		User updated = user.update(
			command.roleId(),
			command.name(),
			command.department(),
			command.email(),
			command.tel()
		);

		userRepository.save(updated);
	}

	public void deleteUser(Long userId, Long tenantId) {
		if (!userRepository.deleteById(userId, tenantId)) {
			throw new CustomException(ErrorCode.USER_NOT_FOUND);
		}
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

		// 역할 존재·부여 가능 여부는 호출부(createUser)의 UserValidator가 이미 확인했다.
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

		// 소속 팀은 로그인 응답에만 필요하므로 인증 principal이 아닌 이 시점에서 1회 조회한다.
		UserTeamSummary team = teamQueryUseCase.getUserTeamSummary(
				authentication.userId(), authentication.tenantId()
		);

		return new SignInResult(
				tokenResult.accessToken(),
				tokenResult.refreshToken(),
				tokenResult.tenantId(),
				tokenResult.tenant(),
				tokenResult.username(),
				tokenResult.name(),
				team == null ? null : team.teamId(),
				team == null ? null : team.teamName(),
				tokenResult.role(),
				tokenResult.refreshTokenValidity()
		);
	}
}
