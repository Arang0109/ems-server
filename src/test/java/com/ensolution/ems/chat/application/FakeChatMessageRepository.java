package com.ensolution.ems.chat.application;

import com.ensolution.ems.chat.application.command.UnreadCursor;
import com.ensolution.ems.chat.application.port.out.ChatMessageRepository;
import com.ensolution.ems.chat.domain.ChatMessage;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 인메모리 {@link ChatMessageRepository}.
 *
 * <p><b>id를 단조 증가 문자열로 채번한다.</b> 실제로는 Mongo가 {@code ObjectId}를 주고 그 16진
 * 문자열의 사전순이 곧 생성순인데, 커서 페이징과 미읽음 집계가 전부 그 성질에 기대고 있다.
 * 여기서도 자릿수를 고정한 0-패딩 문자열을 써서 <b>사전순 = 생성순</b>을 그대로 재현한다 —
 * 이것을 흉내 내지 않으면 커서 경계 테스트가 통과해도 아무것도 보증하지 못한다.
 *
 * <p>tenant 필터와 "자기가 보낸 메시지는 미읽음이 아니다"도 어댑터와 동일하게 재현한다.
 */
public class FakeChatMessageRepository implements ChatMessageRepository {

	private final List<ChatMessage> messages = new ArrayList<>();
	private final AtomicLong sequence = new AtomicLong();

	/** 실패 없이 여러 건을 쌓기 위한 픽스처. 저장 순서가 곧 시간 순서다. */
	public ChatMessage given(Long tenantId, Long roomId, Long senderId, String content) {
		return save(ChatMessage.text(tenantId, roomId, senderId, content, null));
	}

	@Override
	public ChatMessage save(ChatMessage message) {
		ChatMessage saved = message.toBuilder()
			.id(nextId())
			.sentAt(LocalDateTime.now())
			.build();
		messages.add(saved);
		return saved;
	}

	@Override
	public ChatMessage findById(String messageId, Long roomId, Long tenantId) {
		return messages.stream()
			.filter(message -> Objects.equals(message.getId(), messageId))
			.filter(message -> Objects.equals(message.getRoomId(), roomId))
			.filter(message -> Objects.equals(message.getTenantId(), tenantId))
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));
	}

	@Override
	public List<ChatMessage> findPage(Long roomId, Long tenantId, String before, int limit) {
		return messages.stream()
			.filter(message -> Objects.equals(message.getRoomId(), roomId))
			.filter(message -> Objects.equals(message.getTenantId(), tenantId))
			.filter(message -> before == null || message.getId().compareTo(before) < 0)
			.sorted(Comparator.comparing(ChatMessage::getId).reversed())
			.limit(limit)
			.toList();
	}

	/**
	 * 형태가 어긋난 커서를 <b>어댑터와 똑같이 "전부 안 읽음"으로 취급한다.</b>
	 * 실제 어댑터는 여기서 {@code new ObjectId(...)}가 던지면 방 목록 전체가 500이 되고 스스로
	 * 회복되지 않으므로 관용하도록 만들어 두었는데, Fake 가 그것을 흉내 내지 않으면
     * 그 관용을 검증할 방법이 없다.
	 */
	@Override
	public Map<Long, Long> countUnreadByRoom(Long tenantId, Long readerId, List<UnreadCursor> cursors) {
		Map<Long, Long> countByRoomId = new HashMap<>();

		for (UnreadCursor cursor : cursors) {
			boolean countAll = cursor.lastReadMessageId() == null
				|| !ChatMessage.isValidId(cursor.lastReadMessageId());

			long count = messages.stream()
				.filter(message -> Objects.equals(message.getTenantId(), tenantId))
				.filter(message -> Objects.equals(message.getRoomId(), cursor.roomId()))
				.filter(message -> !Objects.equals(message.getSenderId(), readerId))
				.filter(message -> countAll
					|| message.getId().compareTo(cursor.lastReadMessageId()) > 0)
				.count();

			// 어댑터의 $group 도 0인 방은 키를 만들지 않는다.
			if (count > 0) {
				countByRoomId.put(cursor.roomId(), count);
			}
		}
		return countByRoomId;
	}

	/** 사전순이 생성순과 일치하도록 자릿수를 고정한다. */
	private String nextId() {
		return "%024d".formatted(sequence.incrementAndGet());
	}
}
