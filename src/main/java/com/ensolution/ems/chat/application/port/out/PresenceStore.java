package com.ensolution.ems.chat.application.port.out;

import java.util.Collection;
import java.util.Set;

/**
 * 누가 접속해 있는지를 기억하는 저장소.
 *
 * <p><b>휘발성이 본질입니다.</b> 접속 상태를 {@code users} 컬럼에 두면 연결·해제마다 UPDATE 가 나가고,
 * 프로세스가 {@code kill -9} 로 죽으면 그 순간 접속해 있던 사용자가 <b>영원히 온라인</b>으로 남습니다.
 * 구현체는 TTL 을 두어 그 상태가 스스로 사라지게 하고, 살아 있는 세션은 {@link #touch}로 연장합니다.
 *
 * <p>세션 단위로 다루는 이유는 <b>탭을 여러 개 여는 것이 흔하기 때문</b>입니다. 사용자 단위 플래그였다면
 * 탭 하나만 닫아도 오프라인이 됩니다.
 */
public interface PresenceStore {

	/**
	 * 세션 하나를 등록한다.
	 *
	 * @return 이 사용자가 <b>방금 온라인이 되었는가</b>. 첫 세션일 때만 참이며, 그때만 알릴 값어치가 있다
	 */
	boolean connect(Long tenantId, Long userId, String sessionId);

	/**
	 * 세션 하나를 지운다.
	 *
	 * @return 이 사용자가 <b>방금 오프라인이 되었는가</b>. 다른 탭이 남아 있으면 거짓이다
	 */
	boolean disconnect(Long tenantId, Long userId, String sessionId);

	/** 살아 있는 세션의 만료를 미룬다. 갱신하지 않으면 TTL 이 지나 오프라인으로 굳는다. */
	void touch(Long tenantId, Long userId, String sessionId);

	/** 테넌트 안에서 지금 접속 중인 사용자들. 연락처 목록이 조회 한 번으로 상태를 채우는 경로다. */
	Set<Long> onlineUserIds(Long tenantId);

	/** 주어진 사용자들 중 접속 중인 사람. 대화방 목록처럼 대상이 정해진 경우에 쓴다. */
	Set<Long> onlineAmong(Long tenantId, Collection<Long> userIds);
}
