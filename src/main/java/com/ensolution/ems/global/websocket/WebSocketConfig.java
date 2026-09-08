package com.ensolution.ems.global.websocket;

import com.ensolution.ems.global.security.config.CorsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket 전송 계층. <b>채팅 도메인을 알지 못한다</b> — 엔드포인트와 브로커 prefix는
 * 앱 전역 자산이라 {@code global}에 둔다. 나중에 측정계획 편집 알림이 SSE에서 STOMP로 옮겨오더라도
 * 두 번째 설정이 생겨서는 안 된다. 목적지 이름과 페이로드는 이것을 쓰는 기능 모듈이 정한다.
 * <p>
 * <b>서버 → 클라이언트 단방향이다.</b> 클라이언트 {@code SEND}를 받지 않으므로
 * {@code applicationDestinationPrefixes}를 두지 않았고, 쓰기는 전부 REST가 처리한다.
 * 그래야 {@code ApiResponse} 봉투·{@code GlobalExceptionHandler}·{@code @Valid}·multipart를
 * 그대로 쓸 수 있다.
 * <p>
 * <b>{@code /topic}을 열지 않는다.</b> 브로커에 사용자별 큐({@code /queue} + user destination)만 두면
 * 구독 인가가 구조적으로 해결된다 — Spring이 {@code /user/**} 구독을 세션 principal로 치환하므로
 * 클라이언트가 남의 목적지를 구독할 방법이 없다. {@code /topic/chat.room.{roomId}} 같은 공개 목적지를
 * 열었다면 구독마다 참가자인지 확인하는 인터셉터가 필요했을 것이고, 그 인터셉터는 채팅 도메인을
 * 알아야 하므로 이 클래스가 global에 남을 수 없었다.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	/**
	 * 브로커 하트비트 주기(ms). nginx {@code proxy_read_timeout} 기본값(60초)보다 충분히 짧아야
	 * 유휴 연결이 프록시에서 끊기지 않는다. SSE 하트비트를 15초로 잡은 것과 같은 이유이며,
	 * 이 값을 늘릴 때는 프록시 설정을 함께 확인한다.
	 */
	private static final long HEARTBEAT_INTERVAL_MS = 25_000L;

	private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
	private final CorsProperties corsProperties;

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// SockJS 폴백은 쓰지 않는다. 폴백 경로(XHR-streaming 등)가 생기면 토큰 전달과 CORS 처리가
		// 이원화되고, 지원 대상 브라우저에서 네이티브 WebSocket 이 이미 동작한다.
		registry.addEndpoint("/ws")
			.setAllowedOriginPatterns(corsProperties.allowedOrigins().toArray(String[]::new));
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/queue")
			.setHeartbeatValue(new long[]{HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS})
			.setTaskScheduler(brokerTaskScheduler());
		registry.setUserDestinationPrefix("/user");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(stompAuthChannelInterceptor);
	}

	/**
	 * 브로커 하트비트 전용 스케줄러. {@code spring.task.scheduling.pool.size}(= {@code @Scheduled} 풀)를
	 * 공유하지 않는다 — 하트비트가 그 풀을 물면 SSE 하트비트 같은 주기 작업이 함께 밀린다.
	 */
	@Bean
	public TaskScheduler brokerTaskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("stomp-heartbeat-");
		scheduler.setDaemon(true);
		scheduler.initialize();
		return scheduler;
	}
}
