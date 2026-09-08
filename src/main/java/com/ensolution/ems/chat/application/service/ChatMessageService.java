package com.ensolution.ems.chat.application.service;

import com.ensolution.ems.auth.application.port.in.UserQueryUseCase;
import com.ensolution.ems.auth.application.port.in.UserSummary;
import com.ensolution.ems.chat.application.command.ChatMessageListItem;
import com.ensolution.ems.chat.application.command.ChatMessagePage;
import com.ensolution.ems.chat.application.command.SendMessageCommand;
import com.ensolution.ems.chat.application.event.ChatMessagePayload;
import com.ensolution.ems.chat.application.port.out.ChatMessageRepository;
import com.ensolution.ems.chat.application.port.out.ChatParticipantRepository;
import com.ensolution.ems.chat.application.port.out.ChatRoomRepository;
import com.ensolution.ems.chat.application.service.support.ChatEventPublisher;
import com.ensolution.ems.chat.domain.ChatMessage;
import com.ensolution.ems.chat.domain.ChatParticipant;
import com.ensolution.ems.chat.domain.ChatRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 메시지 전송과 대화 이력 조회.
 *
 * <h2>두 저장소에 걸친 쓰기 순서</h2>
 * 방·참가자는 MySQL, 메시지는 MongoDB이고 2PC를 걸 수 없습니다. 순서는 이렇습니다.
 * <pre>
 * ① MySQL  참가자 검증 + 상대의 hidden 해제      ← 롤백된다
 * ② Mongo  메시지 insert                          ← 롤백되지 않는다
 * ③ MySQL  방의 마지막 메시지 요약 갱신           ← 롤백된다
 * ④ 커밋 후 실시간 알림
 * </pre>
 * <b>{@code storage}·{@code schedule}의 "되돌릴 수 없는 쪽을 마지막에"와 순서가 다릅니다.</b>
 * 방의 {@code lastMessageId}는 메시지 id를 참조하는데 그 id는 Mongo가 부여하므로, ③을 ②보다
 * 앞세울 수가 없습니다. 대신 그 규약이 막으려던 것 — <b>MySQL이 존재하지 않는 문서를 가리키는 일</b> —
 * 은 이 순서에서도 일어나지 않습니다. ③이 실패하면 트랜잭션이 롤백되어 방의 요약이 갱신되지 않은
 * 채 메시지만 남으며, 그 메시지는 방을 열면 정상적으로 보이고 다음 전송이 요약을 바로잡습니다.
 * 반대 순서였다면 "요약은 가리키는데 메시지가 없는" 상태가 남습니다.
 * <p>
 * ④가 커밋 이후인 이유는 {@link ChatEventPublisher}에 있습니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageService {

	/** 한 페이지 기본 크기. 클라이언트가 더 큰 값을 요구해도 여기서 자른다. */
	private static final int DEFAULT_PAGE_SIZE = 50;
	private static final int MAX_PAGE_SIZE = 100;

	private final ChatRoomRepository chatRoomRepository;
	private final ChatParticipantRepository chatParticipantRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final UserQueryUseCase userQueryUseCase;
	private final ChatEventPublisher eventPublisher;

	public ChatMessageListItem sendMessage(SendMessageCommand command) {
		// 참가자가 아니면 여기서 CHAT_ROOM_NOT_FOUND 다. 방의 존재조차 알려 주지 않는다.
		chatParticipantRepository.findByRoomIdAndUserId(
			command.roomId(), command.senderId(), command.tenantId());
		ChatRoom room = chatRoomRepository.findById(command.roomId(), command.tenantId());

		// ① 상대가 나가 둔 방이면 다시 드러낸다. 메시지가 왔는데 목록에 없으면 볼 방법이 없다.
		revealForEveryone(command.roomId(), command.tenantId());

		// ② 되돌릴 수 없는 쓰기.
		ChatMessage saved = chatMessageRepository.save(ChatMessage.text(
			command.tenantId(), command.roomId(), command.senderId(),
			command.content(), command.clientMessageId()));

		// ③ 목록에 보여 줄 요약. 메시지 id가 있어야 하므로 ② 뒤에 온다.
		chatRoomRepository.save(room.withLastMessage(saved.getId(), saved.preview(), saved.getSentAt()));

		Map<Long, UserSummary> userById = userById(command.tenantId());
		String senderName = nameOf(userById, command.senderId());

		// ④ 발신자 자신에게도 보낸다 — 다른 탭·기기의 화면을 맞춰야 한다.
		eventPublisher.messageCreated(
			usernamesOf(userById, participantIdsOf(command.roomId(), command.tenantId())),
			toPayload(saved, senderName));

		return toListItem(saved, senderName);
	}

	/**
	 * 대화 이력 한 페이지(최신순).
	 * <p>
	 * 다음 페이지가 있는지 알기 위해 <b>요청 크기보다 한 건 더</b> 읽고, 남는 한 건은 응답에서 뺍니다.
	 * 별도 count 쿼리를 두지 않기 위한 방법입니다.
	 */
	@Transactional(readOnly = true)
	public ChatMessagePage getMessages(Long roomId, Long userId, Long tenantId, String before, Integer size) {
		chatParticipantRepository.findByRoomIdAndUserId(roomId, userId, tenantId);

		int pageSize = normalizeSize(size);
		List<ChatMessage> found = chatMessageRepository.findPage(roomId, tenantId, before, pageSize + 1);

		boolean hasMore = found.size() > pageSize;
		List<ChatMessage> messages = hasMore ? found.subList(0, pageSize) : found;

		Map<Long, UserSummary> userById = userById(tenantId);
		List<ChatMessageListItem> items = messages.stream()
			.map(message -> toListItem(message, nameOf(userById, message.getSenderId())))
			.toList();

		String nextCursor = hasMore ? messages.get(messages.size() - 1).getId() : null;
		return new ChatMessagePage(items, nextCursor, hasMore);
	}

	/**
	 * 상대가 나가 둔 방을 되살린다. 1:1이라 대상은 최대 두 건이고, 이미 보이는 참가자는
	 * {@link ChatParticipant#reveal()}이 같은 인스턴스를 돌려주므로 저장하지 않는다.
	 */
	private void revealForEveryone(Long roomId, Long tenantId) {
		for (ChatParticipant participant : chatParticipantRepository.findAllByRoomId(roomId, tenantId)) {
			if (participant.isHidden()) {
				chatParticipantRepository.save(participant.reveal());
			}
		}
	}

	private List<Long> participantIdsOf(Long roomId, Long tenantId) {
		return chatParticipantRepository.findAllByRoomId(roomId, tenantId).stream()
			.map(ChatParticipant::getUserId)
			.toList();
	}

	private int normalizeSize(Integer size) {
		if (size == null || size <= 0) {
			return DEFAULT_PAGE_SIZE;
		}
		return Math.min(size, MAX_PAGE_SIZE);
	}

	/**
	 * 이름과 username 을 한 번에 얻기 위해 테넌트 전원을 받아 둔다. 이름은 응답에, username 은
	 * 실시간 알림의 수신자 주소에 쓴다({@code TeamAssembler}가 쓰는 방식과 같다).
	 */
	private Map<Long, UserSummary> userById(Long tenantId) {
		return userQueryUseCase.getUserList(tenantId).stream()
			.collect(Collectors.toMap(UserSummary::userId, Function.identity(), (a, b) -> a));
	}

	private static String nameOf(Map<Long, UserSummary> userById, Long userId) {
		UserSummary user = userById.get(userId);
		return user == null ? null : user.name();
	}

	/** 조회되지 않는 사용자(계정 삭제 등)는 수신자에서 빠진다. 보낼 주소가 없다. */
	private static List<String> usernamesOf(Map<Long, UserSummary> userById, List<Long> userIds) {
		return userIds.stream()
			.map(userById::get)
			.filter(Objects::nonNull)
			.map(UserSummary::username)
			.toList();
	}

	private static ChatMessagePayload toPayload(ChatMessage message, String senderName) {
		return new ChatMessagePayload(
			message.getRoomId(),
			message.getId(),
			message.getSenderId(),
			senderName,
			message.getType(),
			message.getContent(),
			message.getClientMessageId(),
			message.getSentAt()
		);
	}

	private static ChatMessageListItem toListItem(ChatMessage message, String senderName) {
		return new ChatMessageListItem(
			message.getId(),
			message.getRoomId(),
			message.getSenderId(),
			senderName,
			message.getType(),
			message.getContent(),
			message.getClientMessageId(),
			message.getSentAt()
		);
	}
}
