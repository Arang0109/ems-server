package com.ensolution.ems.chat.application.port.out;

import com.ensolution.ems.chat.domain.ChatParticipant;

import java.util.List;

public interface ChatParticipantRepository {

	ChatParticipant save(ChatParticipant participant);

	List<ChatParticipant> saveAll(List<ChatParticipant> participants);

	/**
	 * 내가 이 방의 참가자인지 확인하는 경로. <b>없거나 남의 방이면 {@code CHAT_ROOM_NOT_FOUND}</b>다 —
	 * 참가자가 아니라는 사실과 방이 없다는 사실을 구분해 알려 주면 방의 존재가 드러난다(규칙 13).
	 */
	ChatParticipant findByRoomIdAndUserId(Long roomId, Long userId, Long tenantId);

	/** 방의 참가자 전원. 1:1이므로 2건이다. */
	List<ChatParticipant> findAllByRoomId(Long roomId, Long tenantId);

	/**
	 * 여러 방의 참가자 전원을 <b>한 번에</b>.
	 * <p>
	 * 목록 조회가 방마다 {@link #findAllByRoomId}를 부르면 방 수만큼 쿼리가 나간다 — 방 30개면 30번이다.
	 */
	List<ChatParticipant> findAllByRoomIds(List<Long> roomIds, Long tenantId);

	/** 내가 속한 방들. {@code hidden}인 것은 빼고 준다. */
	List<ChatParticipant> findAllVisibleByUserId(Long userId, Long tenantId);

	/** 내가 속한 방들 — 감춘 것까지. 새 메시지가 왔을 때 다시 드러내려면 감춘 것도 봐야 한다. */
	List<ChatParticipant> findAllByUserId(Long userId, Long tenantId);
}
