package com.ensolution.ems.auth.infrastructure.adapter;

import com.ensolution.ems.auth.domain.port.Authenticator;
import com.ensolution.ems.auth.domain.AuthenticatedUser;
import com.ensolution.ems.global.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringSecurityAuthenticator implements Authenticator {
	
	private final AuthenticationManager authenticationManager;
	
	@Override
	public AuthenticatedUser authenticate(String username, String password) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(username, password)
		);
		
		CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
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