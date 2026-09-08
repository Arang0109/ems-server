package com.ensolution.ems.chat.presentation.presence.listener;

import com.ensolution.ems.chat.application.command.ActiveSession;
import com.ensolution.ems.chat.application.service.ChatPresenceService;
import com.ensolution.ems.global.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * STOMP 세션의 생사를 접속 상태로 옮깁니다.
 *
 * <h2>왜 presentation 인가</h2>
 * 세션 이벤트는 <b>외부에서 들어오는 자극</b>이라 Controller 와 같은 구동(driving) 어댑터입니다.
 * {@code StompHeaderAccessor} 에서 {@code (tenantId, userId, sessionId)} 를 꺼내
 * {@link ChatPresenceService} 를 부르는 것이 전부이며, 이는 Controller 가 Request 를 Command 로
 * 바꿔 Service 를 부르는 것과 같은 형태입니다. {@code infrastructure} 에 두면
 * {@code infrastructure → application} 역방향이 되고, {@code application/event/} 는 모듈 자기 이벤트의
 * 자리라 스프링 프레임워크 이벤트가 갈 곳이 아닙니다.
 *
 * <h2>세션 레지스트리를 직접 갖는 이유</h2>
 * 프레즌스 키는 TTL 로 만료되므로 살아 있는 세션은 주기적으로 갱신해야 합니다. 그 대상이 되는
 * "지금 이 프로세스에 붙어 있는 세션" 목록은 여기서만 알 수 있습니다 — 스프링의
 * {@code SimpUserRegistry} 는 username 만 주고 {@code tenantId}·{@code userId} 를 주지 않습니다.
 *
 * <p><b>단일 인스턴스 전제입니다.</b> 이 맵은 프로세스의 힙에만 있습니다. 여러 대로 늘려도
 * 각 인스턴스가 자기 세션만 갱신하면 되므로 이 부분은 그대로 동작하지만, 상태 변화 알림은
 * 자기 프로세스의 브로커에만 가므로 그때는 {@code ChatEventBroadcaster} 뒤에 릴레이가 필요합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSessionEventListener {

	/**
	 * 프레즌스 키 갱신 주기(ms). {@code RedisPresenceStore} 의 TTL(90초)보다 충분히 짧아야
	 * 갱신이 한 번 밀려도 접속 중인 사용자가 오프라인으로 굳지 않는다.
	 */
	private static final long REFRESH_INTERVAL_MS = 30_000L;

	private final Map<String, ActiveSession> sessions = new ConcurrentHashMap<>();

	private final ChatPresenceService chatPresenceService;

	@EventListener
	public void onConnected(SessionConnectedEvent event) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
		ActiveSession session = sessionOf(accessor.getSessionId(), accessor.getUser());
		if (session == null) {
			return;
		}

		sessions.put(session.sessionId(), session);
		chatPresenceService.sessionConnected(session.tenantId(), session.userId(), session.sessionId());
	}

	/**
	 * 끊긴 세션은 <b>레지스트리에서 먼저 꺼낸다.</b> DISCONNECT 프레임에는 principal 이 실려 오지
	 * 않는 경우가 있어(연결이 비정상 종료되면 컨테이너가 만들어 보낸다) 헤더에만 기대면
	 * 누가 나갔는지 알 수 없다.
	 */
	@EventListener
	public void onDisconnected(SessionDisconnectEvent event) {
		ActiveSession session = sessions.remove(event.getSessionId());
		if (session == null) {
			return;
		}
		chatPresenceService.sessionDisconnected(session.tenantId(), session.userId(), session.sessionId());
	}

	/** 살아 있는 세션의 TTL 을 미룬다. 이것이 멈추면 접속 중인 사용자가 90초 뒤 오프라인이 된다. */
	@Scheduled(fixedDelay = REFRESH_INTERVAL_MS)
	public void refreshPresence() {
		if (sessions.isEmpty()) {
			return;
		}
		chatPresenceService.refreshActiveSessions(List.copyOf(sessions.values()));
	}

	/**
	 * 인증되지 않은 세션은 무시한다. {@code StompAuthChannelInterceptor} 가 CONNECT 에서 걸러 내므로
	 * 정상 경로에서는 principal 이 항상 있지만, 없다고 예외를 던져 봐야 알릴 상대가 없다.
	 */
	private static ActiveSession sessionOf(String sessionId, Principal principal) {
		if (sessionId == null || !(principal instanceof Authentication authentication)
			|| !(authentication.getPrincipal() instanceof CustomUserDetails details)) {
			log.debug("[CHAT] 인증 정보가 없는 세션이라 프레즌스를 갱신하지 않습니다. sessionId={}", sessionId);
			return null;
		}
		return new ActiveSession(details.getTenantId(), details.getUserId(), sessionId);
	}
}
