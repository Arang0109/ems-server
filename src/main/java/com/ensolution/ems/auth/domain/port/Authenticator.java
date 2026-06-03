package com.ensolution.ems.auth.domain.port;

import com.ensolution.ems.auth.domain.AuthenticatedUser;

public interface Authenticator {
	AuthenticatedUser authenticate(String username, String password);
}