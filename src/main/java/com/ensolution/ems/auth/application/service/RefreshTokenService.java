package com.ensolution.ems.auth.application.service;

import com.ensolution.ems.auth.application.port.out.RefreshTokenStore;
import com.ensolution.ems.auth.domain.AuthenticatedUser;
import com.ensolution.ems.auth.domain.port.Authenticator;
import com.ensolution.ems.auth.domain.port.TokenIssuer;
import com.ensolution.ems.auth.domain.port.TokenParser;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {
	private final RefreshTokenStore refreshTokenStore;
	private final TokenParser tokenParser;
	private final TokenIssuer tokenIssuer;
	private final Authenticator authenticator;
	
	public void saveRefreshToken(String username, String refreshToken, long duration) {
		refreshTokenStore.save(username, refreshToken, duration);
	}
	
	public String getRefreshToken(String username) {
		return refreshTokenStore.get(username);
	}
	
	public void deleteRefreshToken(String username) {
		refreshTokenStore.delete(username);
	}

	/**
	 * Refresh Token으로 Access Token을 다시 발급합니다.
	 * <p>
	 * Refresh Token 자체는 회전시키지 않습니다 — 프론트는 만료된 요청 여러 건을 동시에
	 * 재시도할 수 있는데, 회전시키면 그중 하나만 성공하고 나머지는 이미 폐기된 토큰을 들고
	 * 실패해 로그아웃됩니다. 대신 저장소의 값과 정확히 일치할 때만 재발급합니다.
	 * <p>
	 * 검증 순서: 서명·만료(JWT) → 저장소 대조(로그아웃 시 삭제되므로 강제 만료 수단이 된다)
	 * → 계정 재조회(그 사이 바뀐 소속·권한을 새 토큰에 반영).
	 *
	 * @throws CustomException 어느 단계든 통과하지 못하면 {@link ErrorCode#REFRESH_TOKEN_INVALID}
	 */
	public String reissueAccessToken(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new CustomException(ErrorCode.REFRESH_TOKEN_INVALID);
		}

		String username = tokenParser.extractUsername(refreshToken)
			.orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_INVALID));

		String storedToken = refreshTokenStore.get(username);
		if (storedToken == null || !storedToken.equals(refreshToken)) {
			throw new CustomException(ErrorCode.REFRESH_TOKEN_INVALID);
		}

		AuthenticatedUser user = authenticator.loadAuthenticatedUser(username)
			.orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_INVALID));

		return tokenIssuer.issueAccessToken(user);
	}
}
