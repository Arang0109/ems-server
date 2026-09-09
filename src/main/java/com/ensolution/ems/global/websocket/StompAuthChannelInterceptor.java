package com.ensolution.ems.global.websocket;

import com.ensolution.ems.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * STOMP {@code CONNECT} 프레임에서 JWT를 검증해 세션 principal을 세운다.
 * <p>
 * <b>핸드셰이크가 아니라 CONNECT에서 인증하는 이유.</b> 브라우저 {@code WebSocket} API는 핸드셰이크
 * HTTP 요청에 커스텀 헤더를 붙일 수 없어 {@code JwtAuthenticationFilter}를 재사용할 수 없다.
 * 토큰을 쿼리 파라미터로 넘기면 액세스 로그·Referer·프록시 로그에 남으므로 채택하지 않았다
 * (프론트가 SSE 구독에서 같은 이유로 {@code EventSource}를 버린 전례가 있다).
 * CONNECT 프레임의 헤더는 <b>WebSocket 페이로드 본문</b>이라 어느 로그에도 남지 않는다.
 * <p>
 * <b>CONNECT에서만 검증한다.</b> {@link JwtTokenProvider#getAuthentication}은 username으로
 * users·roles·tenants를 읽어 DB를 3회 탄다(JWT claim에 userId·tenantId가 없다). 프레임마다 부르면
 * 메시지 한 건에 그 비용이 붙으므로, 연결당 1회만 부르고 이후 프레임은 세션에 붙은 principal을 쓴다.
 * <p>
 * 그 대가로 <b>세션 principal은 CONNECT 시점에 고정된다</b> — 연결 유지 중 토큰이 만료돼도 소켓은
 * 살아 있다. 클라이언트가 액세스 토큰을 갱신할 때마다 재연결하는 것이 계약이며, 서버가 만료 세션을
 * 능동적으로 끊는 것은 아직 하지 않는다(액세스 토큰 유효기간이 1시간이라 노출 창이 제한적이다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
			return message;
		}

		String token = bearerToken(accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION));
		if (token == null || !jwtTokenProvider.validateToken(token)) {
			// 예외를 던지면 클라이언트가 ERROR 프레임을 받고 소켓이 닫힌다. 사유는 남기지 않는다 —
			// 토큰이 없는 것인지 만료인지 구분해 알려 줄 이유가 없다.
			log.debug("[STOMP] 인증되지 않은 CONNECT 를 거부했습니다.");
			throw new MessagingException("UNAUTHORIZED");
		}

		Authentication authentication = jwtTokenProvider.getAuthentication(token);
		accessor.setUser(authentication);
		return message;
	}

	private static String bearerToken(String header) {
		if (header == null || !header.startsWith(BEARER_PREFIX)) {
			return null;
		}
		String token = header.substring(BEARER_PREFIX.length()).trim();
		return token.isEmpty() ? null : token;
	}
}
