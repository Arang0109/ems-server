package com.ensolution.ems.chat.application.service;

import com.ensolution.ems.chat.application.command.ChatRoomDetail;
import com.ensolution.ems.chat.application.command.ChatRoomListItem;
import com.ensolution.ems.chat.application.command.OpenDirectRoomCommand;
import com.ensolution.ems.chat.application.port.out.ChatParticipantRepository;
import com.ensolution.ems.chat.application.port.out.ChatRoomRepository;
import com.ensolution.ems.chat.application.service.assembler.ChatRoomListAssembler;
import com.ensolution.ems.chat.application.service.support.DirectRoomWriter;
import com.ensolution.ems.chat.application.validator.ChatRoomValidator;
import com.ensolution.ems.chat.domain.ChatParticipant;
import com.ensolution.ems.chat.domain.ChatRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 1:1 대화방의 생명주기와 조회.
 * <p>
 * <b>tenant 격리는 두 겹입니다.</b> 방 자신의 tenant 범위와, 개설 시 지정하는 상대가 내 테넌트
 * 소속인지. 둘 중 하나만 빠져도 교차 테넌트가 열립니다 — 후자를 빠뜨리면 다른 고객사 사용자와
 * 대화방이 생기고 이름·부서가 그대로 노출됩니다.
 * <p>
 * 참가자가 아닌 사용자의 접근은 <b>403이 아니라 {@code CHAT_ROOM_NOT_FOUND}</b>입니다(규칙 13).
 * 코드를 나누면 클라이언트가 방의 존재 여부를 알아낼 수 있습니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatParticipantRepository chatParticipantRepository;
	private final ChatRoomValidator chatRoomValidator;
	private final DirectRoomWriter directRoomWriter;
	private final ChatRoomListAssembler assembler;

	/**
	 * 상대와의 대화방을 연다. <b>멱등하다</b> — 이미 있으면 그 방을 돌려준다.
	 * 사용자는 "대화 시작"을 여러 번 눌러도 같은 방으로 들어가야 한다.
	 */
	public ChatRoomDetail openDirectRoom(OpenDirectRoomCommand command) {
		chatRoomValidator.requireNotSelf(command.requesterId(), command.counterpartId());
		chatRoomValidator.requireSameTenantUser(command.counterpartId(), command.tenantId());

		DirectRoomWriter.OpenResult result = directRoomWriter.openOrGet(
			command.tenantId(), command.requesterId(), command.counterpartId());

		ChatParticipant me = chatParticipantRepository.findByRoomIdAndUserId(
			result.room().getId(), command.requesterId(), command.tenantId());

		// 나갔던 방을 다시 열면 내 목록에 되돌린다.
		if (me.isHidden()) {
			me = chatParticipantRepository.save(me.reveal());
		}

		return new ChatRoomDetail(
			result.room().getId(),
			assembler.peerOf(command.tenantId(), command.counterpartId()),
			me.getLastReadMessageId()
		);
	}

	@Transactional(readOnly = true)
	public List<ChatRoomListItem> getRoomList(Long userId, Long tenantId) {
		List<ChatParticipant> myParticipations =
			chatParticipantRepository.findAllVisibleByUserId(userId, tenantId);
		if (myParticipations.isEmpty()) {
			return List.of();
		}

		List<Long> roomIds = myParticipations.stream().map(ChatParticipant::getRoomId).toList();
		List<ChatRoom> rooms = chatRoomRepository.findAllByIds(roomIds, tenantId);
		List<ChatParticipant> allParticipants = roomIds.stream()
			.flatMap(roomId -> chatParticipantRepository.findAllByRoomId(roomId, tenantId).stream())
			.toList();

		return assembler.assemble(tenantId, userId, myParticipations, rooms, allParticipants);
	}

	@Transactional(readOnly = true)
	public ChatRoomDetail getRoom(Long roomId, Long userId, Long tenantId) {
		// 반환값을 쓰지 않는 호출이 아니다 — 내 읽음 커서가 응답에 들어간다.
		ChatParticipant me = chatParticipantRepository.findByRoomIdAndUserId(roomId, userId, tenantId);
		Long peerId = peerIdOf(roomId, userId, tenantId);

		return new ChatRoomDetail(roomId, assembler.peerOf(tenantId, peerId), me.getLastReadMessageId());
	}

	/**
	 * 내 목록에서 방을 감춘다. <b>방을 지우지 않는다</b> — 1:1 대화에서 한쪽이 나갔다고
	 * 상대의 대화 기록까지 사라지면 안 된다. 상대가 새 메시지를 보내면 다시 나타난다.
	 */
	public void hideRoom(Long roomId, Long userId, Long tenantId) {
		ChatParticipant me = chatParticipantRepository.findByRoomIdAndUserId(roomId, userId, tenantId);
		chatParticipantRepository.save(me.hide());
	}

	private Long peerIdOf(Long roomId, Long myUserId, Long tenantId) {
		return chatParticipantRepository.findAllByRoomId(roomId, tenantId).stream()
			.map(ChatParticipant::getUserId)
			.filter(id -> !Objects.equals(id, myUserId))
			.findFirst()
			.orElse(null);
	}
}
