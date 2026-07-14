package com.ensolution.ems.auth.domain;

public record TokenResult (
		String accessToken,
		String refreshToken,
		Long tenantId,
		String tenant,
		String username,
		String name,
		String role,
		Long refreshTokenValidity
) {}
