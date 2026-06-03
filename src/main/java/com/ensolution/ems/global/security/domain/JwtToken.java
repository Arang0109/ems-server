package com.ensolution.ems.global.security.domain;

public record JwtToken(
    String grantType,
    String username,
		String name,
    String accessToken,
    String refreshToken
) {}