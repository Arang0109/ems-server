package com.ensolution.ems.chat.infrastructure.adapter;

import com.ensolution.ems.chat.application.port.out.PresenceStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis 로 접속 상태를 기억하는 어댑터.
 *
 * <h2>키 설계</h2>
 * <pre>
 * PRESENCE:{tenantId}:{userId}   Set  = 그 사용자의 활성 세션 id 들   TTL 90초
 * PRESENCE_TENANT:{tenantId}     Set  = 지금 온라인인 userId 들       TTL 없음(멤버 단위 정리)
 * </pre>
 * 네임스페이스는 {@code auth}의 {@code RT:{username}}과 같은 {@code {용도}:{키}} 형태입니다.
 *
 * <h2>Set 을 쓰는 이유</h2>
 * 사용자 단위 플래그였다면 <b>탭 두 개 중 하나만 닫아도 오프라인</b>이 됩니다. 세션 id 를 원소로 두면
 * 집합이 비었을 때만 오프라인이고, 그 순간에만 알림이 나갑니다.
 *
 * <h2>TTL</h2>
 * 프로세스가 {@code kill -9} 로 죽으면 {@code SessionDisconnectEvent} 가 오지 않아 "영원히 온라인"이
 * 남습니다. 90초 TTL 이 그것을 자동으로 지우고, 살아 있는 세션은 30초 주기 갱신({@code touch})이
 * 만료를 미룹니다. {@code PRESENCE_TENANT} 는 TTL 대신 마지막 세션이 사라질 때 멤버를 뺍니다 —
 * 비정상 종료로 그 정리가 빠지면 온라인 목록에 유령이 남을 수 있어, 조회 시 개별 키의 생존을 함께 봅니다.
 *
 * <h2>단일 인스턴스 전제</h2>
 * 집합 자체는 인스턴스가 늘어도 Redis 에서 합쳐집니다. 합쳐지지 <b>않는</b> 것은 상태 변화 알림입니다 —
 * {@code StompChatEventBroadcaster} 가 자기 프로세스의 브로커에만 보내기 때문입니다.
 * 스케일아웃 시점에는 그쪽에 Redis 릴레이를 끼웁니다.
 *
 * <p>커스텀 직렬화가 필요 없어 {@code RedisConfig} 를 두지 않습니다 — 저장하는 값이 문자열 집합뿐이라
 * 자동설정의 {@link StringRedisTemplate} 으로 충분합니다({@code RedisRefreshTokenStore} 와 같습니다).
 */
@Component
@RequiredArgsConstructor
public class RedisPresenceStore implements PresenceStore {

	/**
	 * 세션 키의 수명. 주기 갱신(30초)보다 충분히 길어야 갱신이 한 번 밀려도 오프라인으로 굳지 않는다.
	 * 이 값을 줄일 때는 갱신 주기를 함께 본다.
	 */
	private static final Duration SESSION_TTL = Duration.ofSeconds(90);

	private static final String SESSION_KEY_PREFIX = "PRESENCE:";
	private static final String TENANT_KEY_PREFIX = "PRESENCE_TENANT:";

	private final StringRedisTemplate redisTemplate;

	@Override
	public boolean connect(Long tenantId, Long userId, String sessionId) {
		String key = sessionKey(tenantId, userId);
		redisTemplate.opsForSet().add(key, sessionId);
		redisTemplate.expire(key, SESSION_TTL);

		// 집합 크기가 1이면 방금 첫 세션이 붙은 것이다.
		boolean becameOnline = size(key) == 1;
		if (becameOnline) {
			redisTemplate.opsForSet().add(tenantKey(tenantId), String.valueOf(userId));
		}
		return becameOnline;
	}

	@Override
	public boolean disconnect(Long tenantId, Long userId, String sessionId) {
		String key = sessionKey(tenantId, userId);
		redisTemplate.opsForSet().remove(key, sessionId);

		if (size(key) > 0) {
			return false;
		}

		redisTemplate.delete(key);
		redisTemplate.opsForSet().remove(tenantKey(tenantId), String.valueOf(userId));
		return true;
	}

	@Override
	public void touch(Long tenantId, Long userId, String sessionId) {
		String key = sessionKey(tenantId, userId);
		// TTL 이 이미 지나 키가 사라졌을 수 있다. 다시 넣어야 접속 중인 사용자가 오프라인으로 굳지 않는다.
		redisTemplate.opsForSet().add(key, sessionId);
		redisTemplate.expire(key, SESSION_TTL);
		redisTemplate.opsForSet().add(tenantKey(tenantId), String.valueOf(userId));
	}

	@Override
	public Set<Long> onlineUserIds(Long tenantId) {
		Set<String> members = redisTemplate.opsForSet().members(tenantKey(tenantId));
		if (members == null || members.isEmpty()) {
			return Set.of();
		}

		return members.stream()
			.map(Long::valueOf)
			// 비정상 종료로 정리되지 않은 멤버를 거른다. 세션 키는 TTL 로 사라졌을 것이다.
			.filter(userId -> alive(tenantId, userId))
			.collect(Collectors.toSet());
	}

	@Override
	public Set<Long> onlineAmong(Long tenantId, Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return Set.of();
		}
		return userIds.stream()
			.filter(Objects::nonNull)
			.filter(userId -> alive(tenantId, userId))
			.collect(Collectors.toSet());
	}

	private boolean alive(Long tenantId, Long userId) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey(tenantId, userId)));
	}

	private long size(String key) {
		Long size = redisTemplate.opsForSet().size(key);
		return size == null ? 0L : size;
	}

	private static String sessionKey(Long tenantId, Long userId) {
		return SESSION_KEY_PREFIX + tenantId + ":" + userId;
	}

	private static String tenantKey(Long tenantId) {
		return TENANT_KEY_PREFIX + tenantId;
	}
}
