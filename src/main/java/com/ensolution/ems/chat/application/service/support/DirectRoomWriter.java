package com.ensolution.ems.chat.application.service.support;

import com.ensolution.ems.chat.application.port.out.ChatParticipantRepository;
import com.ensolution.ems.chat.application.port.out.ChatRoomRepository;
import com.ensolution.ems.chat.domain.ChatParticipant;
import com.ensolution.ems.chat.domain.ChatRoom;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 1:1 대화방의 get-or-create. 동시 개설 정책을 캡슐화한다
 * ({@code schedule}의 {@code SnapshotWriter}와 같은 접미사·같은 이유).
 * <p>
 * <b>"이미 있는지 보고 없으면 만든다"는 애플리케이션 검사로는 성립하지 않는다.</b> 두 사람이 동시에
 * 대화를 시작하면 둘 다 "없다"를 읽고 둘 다 insert 한다. 유일성을 보장하는 것은
 * {@code UNIQUE (tenant_id, pair_key)} 하나뿐이며, 여기서는 그 충돌을 <b>예외가 아니라 신호</b>로
 * 다룬다 — "다른 요청이 방금 만들었다"는 뜻이므로 재조회해서 그 방을 돌려준다.
 * <p>
 * <b>{@code REQUIRES_NEW}인 이유.</b> 호출자의 트랜잭션 안에서 유니크 충돌이 나면 그 트랜잭션이
 * rollback-only로 표시되어, 예외를 잡아 복구해도 커밋 시점에 통째로 죽는다. 별도 트랜잭션으로
 * 분리해야 롤백 범위가 실패한 insert 하나로 끝난다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DirectRoomWriter {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatParticipantRepository chatParticipantRepository;

	/**
	 * 두 사람의 대화방을 얻거나 만든다. 이미 있으면 그 방을 그대로 돌려주므로 <b>멱등</b>하다.
	 *
	 * @return 방과, 그 방이 이번 호출로 새로 생겼는지 여부
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public OpenResult openOrGet(Long tenantId, Long requesterId, Long counterpartId) {
		String pairKey = ChatRoom.pairKeyOf(requesterId, counterpartId);

		return chatRoomRepository.findByPairKey(pairKey, tenantId)
			.map(existing -> new OpenResult(existing, false))
			.orElseGet(() -> create(tenantId, requesterId, counterpartId, pairKey));
	}

	private OpenResult create(Long tenantId, Long requesterId, Long counterpartId, String pairKey) {
		try {
			ChatRoom room = chatRoomRepository.save(ChatRoom.openDirect(tenantId, requesterId, counterpartId));
			chatParticipantRepository.saveAll(List.of(
				ChatParticipant.join(tenantId, room.getId(), requesterId),
				ChatParticipant.join(tenantId, room.getId(), counterpartId)
			));
			return new OpenResult(room, true);
		} catch (DataIntegrityViolationException e) {
			// 경쟁에서 졌다. 상대가 만든 방을 쓰면 되므로 실패가 아니다.
			log.debug("[CHAT] 대화방 동시 개설 충돌을 재조회로 흡수합니다. pairKey={}", pairKey);
			ChatRoom room = chatRoomRepository.findByPairKey(pairKey, tenantId)
				.orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND, "대화방 개설에 실패했습니다.", e));
			return new OpenResult(room, false);
		}
	}

	/** @param created 이번 호출로 방이 새로 생겼는가. 상대에게 "새 대화방" 알림을 보낼지 가른다 */
	public record OpenResult(ChatRoom room, boolean created) {
	}
}
