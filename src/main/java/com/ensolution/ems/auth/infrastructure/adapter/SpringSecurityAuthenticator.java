package com.ensolution.ems.auth.infrastructure.adapter;

import com.ensolution.ems.auth.domain.port.Authenticator;
import com.ensolution.ems.auth.domain.AuthenticatedUser;
import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.security.user.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SpringSecurityAuthenticator implements Authenticator {
	
	private final AuthenticationManager authenticationManager;
	private final CustomUserDetailsService customUserDetailsService;
	
	@Override
	public AuthenticatedUser authenticate(String username, String password) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(username, password)
		);
		
		CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
		return toAuthenticatedUser(principal);
	}

	@Override
	public Optional<AuthenticatedUser> loadAuthenticatedUser(String username) {
		try {
			CustomUserDetails principal =
					(CustomUserDetails) customUserDetailsService.loadUserByUsername(username);
			return Optional.of(toAuthenticatedUser(principal));
		} catch (UsernameNotFoundException e) {
			// 토큰은 살아 있지만 계정이 지워진 경우. 재발급 거절 사유이지 서버 오류가 아니다.
			return Optional.empty();
		}
	}

	private AuthenticatedUser toAuthenticatedUser(CustomUserDetails principal) {
		return new AuthenticatedUser(
				principal.getUserId(),
				principal.getTenantId(),
				principal.getTenant(),
				principal.getUsername(),
				principal.getName(),
				principal.getRole()
		);
	}
}