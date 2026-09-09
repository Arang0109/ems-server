package com.ensolution.ems.auth.infrastructure.adapter;

import com.ensolution.ems.auth.application.port.out.TokenIssuer;
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

		// userId 는 토큰에 담지 않고 결과로만 흘려보낸다. 액세스 토큰 claim 을 바꾸면 이미 발급된
		// 토큰과 해석이 갈리고, principal 은 어차피 매 요청 DB 에서 만들어지므로 얻는 것이 없다.
		return new TokenResult(
			user.userId(), accessToken, refreshToken, tenantId, tenant,
			username, name, role,
			jwtProperties.refreshTokenValidity()
		);
	}

	@Override
	public String issueAccessToken(AuthenticatedUser user) {
		return jwtTokenProvider.createAccessToken(user.username(), user.tenant(), user.role());
	}
}