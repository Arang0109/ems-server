package com.ensolution.ems.auth.domain;

public record TokenResult (
		String accessToken,
		String refreshToken,
		String username,
		String name,
		Long refreshTokenValidity
) {}