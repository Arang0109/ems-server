package com.ensolution.ems.auth.application;

import com.ensolution.ems.auth.application.port.RefreshTokenStore;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {
	private final RefreshTokenStore refreshTokenStore;
	
	public void saveRefreshToken(String username, String refreshToken, long duration) {
		refreshTokenStore.save(username, refreshToken, duration);
	}
	
	public String getRefreshToken(String username) {
		return refreshTokenStore.get(username);
	}
	
	public void deleteRefreshToken(String username) {
		refreshTokenStore.delete(username);
	}
}