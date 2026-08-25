package com.ensolution.ems.auth.infrastructure.adapter;

import com.ensolution.ems.auth.domain.port.TokenIssuer;
import com.ensolution.ems.auth.domain.AuthenticatedUser;
import com.ensolution.ems.auth.domain.TokenResult;
import com.ensolution.ems.global.security.domain.JwtProperties;
import com.ensolution.ems.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenIssuer implements TokenIssuer {
	
	private final JwtTokenProvider jwtTokenProvider;
	private final JwtProperties jwtProperties;
	
	@Override
	public TokenResult issue(AuthenticatedUser user) {

		String username = user.username();
		String name = user.name();
		Long tenantId = user.tenantId();
		String tenant = user.tenant();
		String role = user.role();

		String accessToken = issueAccessToken(user);
		String refreshToken = jwtTokenProvider.createRefreshToken(username);

		return new TokenResult(
			accessToken,
			refreshToken,
			tenantId,
			tenant,
			username,
			name,
			role,
			jwtProperties.refreshTokenValidity()
		);
	}

	@Override
	public String issueAccessToken(AuthenticatedUser user) {
		return jwtTokenProvider.createAccessToken(user.username(), user.tenant(), user.role());
	}
}