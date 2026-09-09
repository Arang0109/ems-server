package com.ensolution.ems.chat.application.port.out;

import com.ensolution.ems.chat.domain.ChatRoom;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository {

	/**
	 * <b>유니크 충돌을 삼키지 않는다.</b> {@code (tenant_id, pair_key)} 제약을 어기면
	 * {@code DataIntegrityViolationException}이 그대로 올라오며, 그것을 "다른 요청이 방금 만들었다"로
	 * 해석해 재조회하는 것은 {@code DirectRoomWriter}의 몫이다.
	 */
	ChatRoom save(ChatRoom room);

	ChatRoom findById(Long roomId, Long tenantId);

	/** 방 개설의 get-or-create 경로에서만 쓴다. 없는 것이 정상이므로 여기만 {@code Optional}이다. */
	Optional<ChatRoom> findByPairKey(String pairKey, Long tenantId);

	/** 최근 대화 순. 목록에 보일 방은 참가자 쪽에서 걸러 온 id들이다. */
	List<ChatRoom> findAllByIds(List<Long> roomIds, Long tenantId);
}
