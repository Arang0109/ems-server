package com.ensolution.ems.auth.domain.port;

import java.util.Optional;

/**
 * 발급한 토큰에서 주체(username)를 읽어내는 포트입니다.
 * <p>
 * 서명이 맞지 않거나 만료된 토큰은 예외 대신 {@link Optional#empty()} 로 돌려줍니다 —
 * "토큰을 신뢰할 수 없다"는 사실만 전달하고, 그것을 어떤 에러로 볼지는 호출부가 정합니다.
 */
public interface TokenParser {
	Optional<String> extractUsername(String token);
}
