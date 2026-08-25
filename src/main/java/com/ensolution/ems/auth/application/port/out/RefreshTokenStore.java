package com.ensolution.ems.auth.application.port.out;

public interface RefreshTokenStore {
	void save(String username, String refreshToken, long durationMillis);
	String get(String username);
	void delete(String username);
}