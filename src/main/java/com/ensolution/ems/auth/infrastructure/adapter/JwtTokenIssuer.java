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
		
		String accessToken = jwtTokenProvider.createAccessToken(username);
		String refreshToken = jwtTokenProvider.createRefreshToken(username);
		
		return new TokenResult(
				accessToken,
				refreshToken,
				username,
				name,
				jwtProperties.refreshTokenValidity()
		);
	}
}