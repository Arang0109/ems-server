package com.ensolution.ems.auth.domain.port;

import com.ensolution.ems.auth.domain.AuthenticatedUser;

import java.util.Optional;

public interface Authenticator {
	AuthenticatedUser authenticate(String username, String password);

	/**
	 * 비밀번호 확인 없이 인증 주체를 다시 읽어옵니다.
	 * <p>
	 * Refresh Token이 이미 신원을 증명한 뒤, 그 사이 바뀌었을 수 있는 소속·권한을
	 * 새 Access Token에 반영하기 위해 최신 상태를 다시 조회하는 용도입니다.
	 * 계정이 사라졌다면 빈 값입니다.
	 */
	Optional<AuthenticatedUser> loadAuthenticatedUser(String username);
}