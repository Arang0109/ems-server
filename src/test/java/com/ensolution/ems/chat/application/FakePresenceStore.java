package com.ensolution.ems.chat.application;

import com.ensolution.ems.chat.application.port.out.PresenceStore;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 인메모리 {@link PresenceStore}.
 *
 * <p><b>세션 집합을 실제 구현과 같은 형태로 재현한다.</b> 사용자 단위 플래그로 흉내 내면
 * "탭 두 개 중 하나만 닫으면 여전히 온라인"이라는 규칙을 검증할 수 없다 — 그 규칙이 Set 을 쓰는
 * 유일한 이유이기도 하다.
 *
 * <p>TTL 은 재현하지 않는다. 시간에 의존하는 테스트는 느리고 불안정하며, 만료 자체는 Redis 의
 * 책임이라 여기서 고정할 것이 없다. {@link #touch}는 호출만 기록한다.
 */
public class FakePresenceStore implements PresenceStore {

	private final Map<Key, Set<String>> sessionsByUser = new HashMap<>();
	private final Set<Key> touched = new LinkedHashSet<>();

	private record Key(Long tenantId, Long userId) {
	}

	/** {@link #touch}가 닿은 사용자들. 주기 갱신이 실제로 대상을 훑었는지 확인한다. */
	public Set<Long> touchedUserIds() {
		return touched.stream().map(Key::userId).collect(Collectors.toSet());
	}

	@Override
	public boolean connect(Long tenantId, Long userId, String sessionId) {
		Set<String> sessions =
			sessionsByUser.computeIfAbsent(new Key(tenantId, userId), key -> new HashSet<>());
		sessions.add(sessionId);
		return sessions.size() == 1;
	}

	@Override
	public boolean disconnect(Long tenantId, Long userId, String sessionId) {
		Key key = new Key(tenantId, userId);
		Set<String> sessions = sessionsByUser.get(key);
		if (sessions == null) {
			return false;
		}

		sessions.remove(sessionId);
		if (sessions.isEmpty()) {
			sessionsByUser.remove(key);
			return true;
		}
		return false;
	}

	@Override
	public void touch(Long tenantId, Long userId, String sessionId) {
		touched.add(new Key(tenantId, userId));
		sessionsByUser.computeIfAbsent(new Key(tenantId, userId), key -> new HashSet<>()).add(sessionId);
	}

	@Override
	public Set<Long> onlineUserIds(Long tenantId) {
		return sessionsByUser.keySet().stream()
			.filter(key -> Objects.equals(key.tenantId(), tenantId))
			.map(Key::userId)
			.collect(Collectors.toSet());
	}

	@Override
	public Set<Long> onlineAmong(Long tenantId, Collection<Long> userIds) {
		Set<Long> online = onlineUserIds(tenantId);
		return userIds.stream().filter(online::contains).collect(Collectors.toSet());
	}
}
