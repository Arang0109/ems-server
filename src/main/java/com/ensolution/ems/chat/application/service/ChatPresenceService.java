package com.ensolution.ems.chat.application.service;

import com.ensolution.ems.auth.application.port.in.UserQueryUseCase;
import com.ensolution.ems.auth.application.port.in.UserSummary;
import com.ensolution.ems.chat.application.command.ActiveSession;
import com.ensolution.ems.chat.application.command.ChatContactListItem;
import com.ensolution.ems.chat.application.event.ChatPresencePayload;
import com.ensolution.ems.chat.application.port.out.PresenceStore;
import com.ensolution.ems.chat.application.service.support.ChatEventPublisher;
import com.ensolution.ems.chat.domain.PresenceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 접속 상태와 대화 상대 목록.
 *
 * <h2>상태 변화만 알린다</h2>
 * 탭을 하나 더 여는 것은 상태 변화가 아닙니다. {@link PresenceStore}가 "방금 온라인이 되었는가"를
 * 답해 주고, 참일 때만 알림이 나갑니다. 그러지 않으면 탭을 열고 닫을 때마다 테넌트 전원에게
 * 프레임이 날아갑니다.
 *
 * <h2>연락처 목록의 조회 횟수</h2>
 * 사용자 조회 1회 + 온라인 집합 1회로 <b>고정</b>입니다. 사람 수만큼 상태를 물으면 N+1 이 됩니다.
 *
 * <h2>왜 별도 서비스인가</h2>
 * 애그리거트 축 분할입니다. 프레즌스는 방·메시지와 저장소도(Redis) 수명도(세션) 다르고,
 * 유일하게 {@link PresenceStore}에 의존합니다. CQRS 축 분할이 아닙니다.
 */
@Service
@RequiredArgsConstructor
public class ChatPresenceService {

	private final PresenceStore presenceStore;
	private final UserQueryUseCase userQueryUseCase;
	private final ChatEventPublisher eventPublisher;

	/** 세션이 붙었다. 첫 세션일 때만 테넌트에 알린다. */
	public void sessionConnected(Long tenantId, Long userId, String sessionId) {
		if (presenceStore.connect(tenantId, userId, sessionId)) {
			broadcast(tenantId, userId, PresenceStatus.ONLINE);
		}
	}

	/** 세션이 끊겼다. 마지막 세션일 때만 알린다 — 탭 하나를 닫은 것은 오프라인이 아니다. */
	public void sessionDisconnected(Long tenantId, Long userId, String sessionId) {
		if (presenceStore.disconnect(tenantId, userId, sessionId)) {
			broadcast(tenantId, userId, PresenceStatus.OFFLINE);
		}
	}

	/** 살아 있는 세션의 만료를 미룬다. 이것이 멈추면 접속 중인 사용자가 TTL 뒤에 오프라인으로 굳는다. */
	public void refreshActiveSessions(List<ActiveSession> sessions) {
		for (ActiveSession session : sessions) {
			presenceStore.touch(session.tenantId(), session.userId(), session.sessionId());
		}
	}

	/**
	 * 대화를 시작할 수 있는 사람들. 자기 자신은 뺀다.
	 * <p>
	 * {@code /api/admin/members} 를 재사용하지 않는 이유는 그쪽이 ADMIN 전용이고 {@code email}·
	 * {@code tel} 까지 내보내기 때문입니다. 대화 상대를 고르는 데 필요한 것만 담습니다.
	 */
	@Transactional(readOnly = true)
	public List<ChatContactListItem> getContacts(Long myUserId, Long tenantId) {
		Set<Long> online = presenceStore.onlineUserIds(tenantId);

		return userQueryUseCase.getUserList(tenantId).stream()
			.filter(user -> !Objects.equals(user.userId(), myUserId))
			.map(user -> toListItem(user, online.contains(user.userId())))
			.toList();
	}

	/**
	 * 상태 변화를 <b>같은 테넌트의 접속 중인 사람들</b>에게 보낸다.
	 * <p>
	 * 연락처 목록이 테넌트 전원을 보여 주므로 관심 대상도 전원이다. 구독 관계 그래프를 따로 두면
	 * 유지 비용이 이득을 넘는다. 실제 팬아웃 크기는 <b>동시 접속자 수</b>라 수십 건 수준이고,
	 * 접속하지 않은 사람에게 보내 봐야 브로커가 버릴 뿐이다.
	 */
	private void broadcast(Long tenantId, Long userId, PresenceStatus status) {
		// 온라인 집합을 한 번만 읽는다. 사람마다 물으면 상태 변화 한 건에 조회가 N 번 나간다.
		Set<Long> online = presenceStore.onlineUserIds(tenantId);

		List<String> recipients = userQueryUseCase.getUserList(tenantId).stream()
			.filter(user -> !Objects.equals(user.userId(), userId))
			.filter(user -> online.contains(user.userId()))
			.map(UserSummary::username)
			.toList();

		eventPublisher.presenceChanged(recipients,
			new ChatPresencePayload(userId, status, LocalDateTime.now()));
	}

	private static ChatContactListItem toListItem(UserSummary user, boolean online) {
		return new ChatContactListItem(
			user.userId(), user.name(), user.department(), user.role(), online);
	}
}
