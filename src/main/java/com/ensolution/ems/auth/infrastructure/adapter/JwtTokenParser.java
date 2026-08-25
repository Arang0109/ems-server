package com.ensolution.ems.auth.infrastructure.adapter;

import com.ensolution.ems.auth.domain.port.TokenParser;
import com.ensolution.ems.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtTokenParser implements TokenParser {

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	public Optional<String> extractUsername(String token) {
		return jwtTokenProvider.parseUsername(token);
	}
}
