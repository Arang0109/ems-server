package com.ensolution.ems.auth.application;

import com.ensolution.ems.auth.application.command.SignInCommand;
import com.ensolution.ems.auth.application.command.SignInResult;
import com.ensolution.ems.auth.application.command.SignUpCommand;
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
public class AuthService {
	private final UserRepository userRepository;
	private final PasswordEncryptor passwordEncryptor;
	private final Authenticator authenticator;
	private final TokenIssuer tokenIssuer;
	
	public void signUp(SignUpCommand command) {
		if (userRepository.existsByUsername(command.username())) {
			throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
		};
		
		String encodedPassword = passwordEncryptor.encode(command.password());
		
		User user = User.signUp(
			command.username(),
			encodedPassword,
			command.department(),
			command.name(),
			command.email(),
			command.tel()
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
				tokenResult.username(),
				tokenResult.name(),
				tokenResult.refreshTokenValidity()
		);
	}
}
