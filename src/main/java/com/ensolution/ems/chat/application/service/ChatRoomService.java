package com.ensolution.ems.chat.application.service;

import com.ensolution.ems.auth.application.port.in.UserQueryUseCase;
import com.ensolution.ems.auth.application.port.in.UserSummary;
import com.ensolution.ems.chat.application.command.ChatRoomDetail;
import com.ensolution.ems.chat.application.command.ChatRoomListItem;
import com.ensolution.ems.chat.application.command.MarkAsReadCommand;
import com.ensolution.ems.chat.application.command.OpenDirectRoomCommand;
import com.ensolution.ems.chat.application.event.ChatReadPayload;
import com.ensolution.ems.chat.application.event.ChatRoomOpenedPayload;
import com.ensolution.ems.chat.application.port.out.ChatParticipantRepository;
import com.ensolution.ems.chat.application.port.out.ChatRoomRepository;
import com.ensolution.ems.chat.application.service.assembler.ChatRoomListAssembler;
import com.ensolution.ems.chat.application.service.support.ChatEventPublisher;
import com.ensolution.ems.chat.application.service.support.DirectRoomWriter;
import com.ensolution.ems.chat.application.validator.ChatRoomValidator;
import com.ensolution.ems.chat.domain.ChatParticipant;
import com.ensolution.ems.chat.domain.ChatRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
	private final UserQueryUseCase userQueryUseCase;
	private final ChatEventPublisher eventPublisher;

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

		// 상대가 첫 메시지 전에도 목록에서 방을 볼 수 있어야 한다. 연 사람에게는 보내지 않는다 —
		// 그쪽은 이 REST 응답으로 이미 방을 받았다.
		if (result.created()) {
			notifyRoomOpened(command, result.room().getId());
		}

		return toDetail(
			result.room().getId(),
			command.tenantId(),
			me,
			peerOf(result.room().getId(), command.requesterId(), command.tenantId())
		);
	}

	@Transactional(readOnly = true)
	public List<ChatRoomListItem> getRoomList(Long userId, Long tenantId) {
		List<ChatParticipant> myParticipation =
			chatParticipantRepository.findAllVisibleByUserId(userId, tenantId);
		if (myParticipation.isEmpty()) {
			return List.of();
		}

		List<Long> roomIds = myParticipation.stream().map(ChatParticipant::getRoomId).toList();
		List<ChatRoom> rooms = chatRoomRepository.findAllByIds(roomIds, tenantId);
		// 방마다 묻지 않는다 — 방 30개면 쿼리가 30번 나가고, 어셈블러가 "조회 횟수 고정"이라고
		// 적어 둔 약속이 여기서 깨진다.
		List<ChatParticipant> allParticipants =
			chatParticipantRepository.findAllByRoomIds(roomIds, tenantId);

		return assembler.assemble(tenantId, userId, myParticipation, rooms, allParticipants);
	}

	@Transactional(readOnly = true)
	public ChatRoomDetail getRoom(Long roomId, Long userId, Long tenantId) {
		// 반환값을 쓰지 않는 호출이 아니다 — 내 읽음 커서가 응답에 들어간다.
		ChatParticipant me = chatParticipantRepository.findByRoomIdAndUserId(roomId, userId, tenantId);
		ChatParticipant peer = peerOf(roomId, userId, tenantId);

		return toDetail(roomId, tenantId, me, peer);
	}

	/**
	 * 내 목록에서 방을 감춘다. <b>방을 지우지 않는다</b> — 1:1 대화에서 한쪽이 나갔다고
	 * 상대의 대화 기록까지 사라지면 안 된다. 상대가 새 메시지를 보내면 다시 나타난다.
	 */
	public void hideRoom(Long roomId, Long userId, Long tenantId) {
		ChatParticipant me = chatParticipantRepository.findByRoomIdAndUserId(roomId, userId, tenantId);
		chatParticipantRepository.save(me.hide());
	}

	/**
	 * 읽음 위치를 보고한다. <b>커서는 앞으로만 간다</b> — 위로 스크롤해 옛 메시지를 보는 것은
	 * 읽음 취소가 아니고, 여러 탭의 보고가 뒤바뀐 순서로 도착할 수도 있다. 되돌아가면 이미 읽은
	 * 메시지가 다시 안 읽음으로 살아난다.
	 *
	 * @return 커서가 실제로 움직였는가. 움직였을 때만 상대에게 읽음 확인을 알릴 값어치가 있다
	 */
	public boolean markAsRead(MarkAsReadCommand command) {
		ChatParticipant me = chatParticipantRepository.findByRoomIdAndUserId(
			command.roomId(), command.readerId(), command.tenantId());

		ChatParticipant read = me.readUpTo(command.lastReadMessageId(), LocalDateTime.now());
		if (read == me) {
			return false;
		}

		chatParticipantRepository.save(read);

		// 읽은 사람 자신에게는 보내지 않는다 — 자기 화면은 이미 알고 있다.
		notifyRead(command, read);
		return true;
	}

	/** 전역 배지용 합계. 감춰 둔 방은 세지 않는다 — 목록에 없는 방의 배지는 눌러 볼 곳이 없다. */
	@Transactional(readOnly = true)
	public long getTotalUnreadCount(Long userId, Long tenantId) {
		return assembler.totalUnread(tenantId, userId,
			chatParticipantRepository.findAllVisibleByUserId(userId, tenantId));
	}

	/** 상대 입장에서는 "요청자"가 대화 상대다. 그 관점으로 payload 를 만든다. */
	private void notifyRoomOpened(OpenDirectRoomCommand command, Long roomId) {
		UserSummary requester = userQueryUseCase.getUser(command.requesterId(), command.tenantId());
		UserSummary counterpart = userQueryUseCase.getUser(command.counterpartId(), command.tenantId());

		eventPublisher.roomOpened(counterpart.username(), new ChatRoomOpenedPayload(
			roomId, requester.userId(), requester.name(), requester.department()));
	}

	private void notifyRead(MarkAsReadCommand command, ChatParticipant read) {
		ChatParticipant peer = peerOf(command.roomId(), command.readerId(), command.tenantId());
		if (peer == null) {
			return;
		}
		eventPublisher.messagesRead(
			userQueryUseCase.getUser(peer.getUserId(), command.tenantId()).username(),
			new ChatReadPayload(command.roomId(), command.readerId(),
				read.getLastReadMessageId(), read.getLastReadAt()));
	}

	/**
	 * 방 단건 응답. <b>두 커서를 각자의 자리에</b> 담는다 — 뒤바뀌면 내가 읽은 위치가 상대의 읽음
	 * 표시로 그려져, 상대가 읽지 않은 메시지에 "읽음"이 붙는다.
	 */
	private ChatRoomDetail toDetail(Long roomId, Long tenantId, ChatParticipant me, ChatParticipant peer) {
		return new ChatRoomDetail(
			roomId,
			peer == null ? null : assembler.peerOf(tenantId, peer.getUserId()),
			me.getLastReadMessageId(),
			peer == null ? null : peer.getLastReadMessageId()
		);
	}

	/**
	 * 상대 참가자. <b>id 가 아니라 행을 돌려주는 이유</b>는 읽음 커서가 그 행에 있기 때문이다 —
	 * id 만 받아 오면 커서를 얻으려고 같은 행을 한 번 더 읽게 된다.
	 * <p>
	 * 계정 삭제 등으로 상대 행이 없을 수 있으므로 {@code null} 을 돌려준다. 대화 기록은 남아야 하고,
	 * 상대가 사라졌다고 방 조회가 실패해서는 안 된다.
	 */
	private ChatParticipant peerOf(Long roomId, Long myUserId, Long tenantId) {
		return chatParticipantRepository.findAllByRoomId(roomId, tenantId).stream()
			.filter(participant -> !Objects.equals(participant.getUserId(), myUserId))
			.findFirst()
			.orElse(null);
	}
}
