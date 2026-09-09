package com.ensolution.ems.global.websocket;

import com.ensolution.ems.global.security.jwt.JwtTokenProvider;
import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.security.user.CustomUserDetailsService;
import com.ensolution.ems.global.security.domain.JwtProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * WebSocket 세션의 인증 관문이 지키는 것을 고정한다.
 *
 * <p>이 인터셉터가 유일한 방어선이다. 핸드셰이크({@code /ws/**})는 {@code SecurityConfig}에서
 * permitAll 이므로, 여기서 통과시킨 세션은 그대로 목적지를 구독한다. <b>토큰이 없거나 무효인
 * CONNECT 가 통과하면 인증 없는 세션이 열린다.</b>
 *
 * <p>반대로 <b>{@code CONNECT} 외의 프레임에서는 아무것도 하지 않아야</b> 한다. 매 프레임 검증하면
 * {@code getAuthentication()}이 타는 DB 3회(users·roles·tenants)가 메시지마다 붙는다. 인증은
 * 연결당 1회이고 이후는 세션에 붙은 principal 을 쓴다는 설계를 구조로 고정한다.
 */
class StompAuthChannelInterceptorTest {

	private static final String VALID_TOKEN = "valid-token";

	private final StubJwtTokenProvider jwtTokenProvider = new StubJwtTokenProvider();
	private final StompAuthChannelInterceptor interceptor = new StompAuthChannelInterceptor(jwtTokenProvider);

	private static Message<byte[]> connectFrame(String authorizationHeader) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		if (authorizationHeader != null) {
			accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
		}
		accessor.setLeaveMutable(true);
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}

	private static Message<byte[]> frame(StompCommand command) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
		accessor.setLeaveMutable(true);
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}

	@Nested
	@DisplayName("CONNECT — 인증 관문")
	class Connect {

		@Test
		@DisplayName("유효한 토큰이면 세션에 principal 이 붙는다")
		void 유효한_토큰은_principal을_세운다() {
			Message<?> result = interceptor.preSend(connectFrame("Bearer " + VALID_TOKEN), null);

			StompHeaderAccessor accessor =
				StompHeaderAccessor.wrap(result);
			assertThat(accessor.getUser()).isNotNull();
			assertThat(accessor.getUser().getName()).isEqualTo("member");
			assertThat(jwtTokenProvider.authenticationCalls).isEqualTo(1);
		}

		@Test
		@DisplayName("Authorization 헤더가 없으면 연결을 거부한다")
		void 헤더가_없으면_거부한다() {
			assertThatThrownBy(() -> interceptor.preSend(connectFrame(null), null))
				.isInstanceOf(MessagingException.class);
		}

		@Test
		@DisplayName("Bearer 접두어가 없으면 연결을 거부한다")
		void 접두어가_없으면_거부한다() {
			assertThatThrownBy(() -> interceptor.preSend(connectFrame(VALID_TOKEN), null))
				.isInstanceOf(MessagingException.class);
		}

		@Test
		@DisplayName("접두어만 있고 토큰이 비면 연결을 거부한다")
		void 빈_토큰은_거부한다() {
			assertThatThrownBy(() -> interceptor.preSend(connectFrame("Bearer   "), null))
				.isInstanceOf(MessagingException.class);
		}

		@Test
		@DisplayName("서명·만료가 어긋난 토큰이면 연결을 거부하고 주체를 조회하지 않는다")
		void 무효한_토큰은_거부한다() {
			assertThatThrownBy(() -> interceptor.preSend(connectFrame("Bearer expired"), null))
				.isInstanceOf(MessagingException.class);

			// 검증에서 걸렀으므로 DB 를 타는 조회까지 가지 않는다.
			assertThat(jwtTokenProvider.authenticationCalls).isZero();
		}
	}

	@Nested
	@DisplayName("CONNECT 외 프레임 — 손대지 않는다")
	class OtherFrames {

		@Test
		@DisplayName("SUBSCRIBE 는 토큰이 없어도 통과하며 주체를 다시 조회하지 않는다")
		void 구독은_재인증하지_않는다() {
			Message<byte[]> subscribe = frame(StompCommand.SUBSCRIBE);

			assertThatCode(() -> interceptor.preSend(subscribe, null)).doesNotThrowAnyException();
			assertThat(jwtTokenProvider.authenticationCalls).isZero();
		}

		@Test
		@DisplayName("DISCONNECT 도 마찬가지다")
		void 연결종료는_재인증하지_않는다() {
			assertThatCode(() -> interceptor.preSend(frame(StompCommand.DISCONNECT), null))
				.doesNotThrowAnyException();
			assertThat(jwtTokenProvider.authenticationCalls).isZero();
		}
	}

	/**
	 * 토큰 해석만 흉내 내는 {@link JwtTokenProvider}. 서명 키·DB 없이 "유효한가"와
	 * "몇 번 주체를 조회했는가"만 재현한다 — 이 테스트의 관심사가 그 둘이다.
	 */
	private static class StubJwtTokenProvider extends JwtTokenProvider {

		private int authenticationCalls;

		StubJwtTokenProvider() {
			super(new JwtProperties("test-secret-key-must-be-long-enough-for-hmac-sha", 3600000L, 25200000L),
				new CustomUserDetailsService(null, null));
		}

		@Override
		public boolean validateToken(String token) {
			return VALID_TOKEN.equals(token);
		}

		@Override
		public org.springframework.security.core.Authentication getAuthentication(String token) {
			authenticationCalls++;
			UserDetails details = new CustomUserDetails(
				7L, 1L, "엔솔루션", "member", "encoded-pw", "홍길동",
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
			return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
				details, null, details.getAuthorities());
		}
	}
}
