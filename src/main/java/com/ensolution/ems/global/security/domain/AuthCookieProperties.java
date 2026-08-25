package com.ensolution.ems.global.security.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Refresh Token 쿠키의 전송 정책입니다.
 * <p>
 * {@code secure=true} 쿠키는 HTTPS 응답에서만 브라우저에 저장되므로, HTTP로 접속하는
 * 개발 서버에서는 로그인 직후 쿠키가 조용히 버려져 재발급이 영영 실패합니다.
 * 프론트와 API는 nginx가 같은 오리진으로 묶어 주므로 기본값은 {@code Lax + secure=false} 이고,
 * HTTPS 도메인으로 서비스할 때만 환경변수로 {@code None + secure=true} 로 올립니다.
 */
@ConfigurationProperties(prefix = "app.cookie")
public record AuthCookieProperties(
	boolean secure,
	String sameSite
) {
}
