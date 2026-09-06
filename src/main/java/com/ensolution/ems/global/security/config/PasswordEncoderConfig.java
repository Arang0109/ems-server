package com.ensolution.ems.global.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 인코더. <b>{@code SecurityConfig}와 일부러 분리했다.</b>
 * <p>
 * 이 빈은 필터 체인과 아무 관계가 없는데도 {@code SecurityConfig}에 있으면 다음 순환이 생긴다:
 * <pre>
 * SecurityConfig → JwtAuthenticationFilter → JwtTokenProvider → CustomUserDetailsService
 *               → PlatformService(TenantQueryUseCase) → AuthService(UserCommandUseCase)
 *               → BCryptPasswordEncryptor → PasswordEncoder ─┐
 *               ←──────────────────────────────────────────┘
 * </pre>
 * 인코더를 여기로 빼면 {@code BCryptPasswordEncryptor}가 보안 설정 전체가 아니라 이 작은 설정만
 * 바라보므로 고리가 끊어진다. {@code @Lazy}로 덮는 대신 원인을 없앤 것이다.
 * <p>
 * <b>여기에 다른 빈을 추가하지 말 것.</b> 설정이 커지면 같은 순환이 되살아난다.
 */
@Configuration
public class PasswordEncoderConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
