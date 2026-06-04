package com.ensolution.ems.auth.domain.port;

import com.ensolution.ems.auth.domain.AuthenticatedUser;
import com.ensolution.ems.auth.domain.TokenResult;

public interface TokenIssuer {
	TokenResult issue(AuthenticatedUser user);
}