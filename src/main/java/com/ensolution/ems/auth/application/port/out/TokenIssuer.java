package com.ensolution.ems.auth.application.port.out;

import com.ensolution.ems.auth.domain.AuthenticatedUser;
import com.ensolution.ems.auth.domain.TokenResult;

public interface TokenIssuer {
	TokenResult issue(AuthenticatedUser user);

	/**
	 * Access Token만 새로 발급합니다. Refresh Token으로 세션을 이어갈 때 씁니다.
	 */
	String issueAccessToken(AuthenticatedUser user);
}