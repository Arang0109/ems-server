package com.ensolution.ems.chat.application;

import com.ensolution.ems.chat.application.event.ChatMessagePayload;
import com.ensolution.ems.chat.application.event.ChatReadPayload;
import com.ensolution.ems.chat.application.event.ChatRoomOpenedPayload;
import com.ensolution.ems.chat.application.port.out.ChatEventBroadcaster;

import java.util.ArrayList;
import java.util.List;

/**
 * 발행된 알림을 기록하는 {@link ChatEventBroadcaster}.
 * {@code DocumentServiceTest}의 {@code RecordingFileStorage}와 같은 형태다.
 *
 * <p>기록하는 것은 <b>수신자와 페이로드 쌍</b>이다. 채팅에서 누구에게 보내는가는 페이로드만큼
 * 중요한 계약이라 — 상대에게 가야 할 것이 안 가거나, 가지 말아야 할 사람에게 가는 것이 곧 결함이다.
 */
public class RecordingChatEventBroadcaster implements ChatEventBroadcaster {

	public record Sent<T>(List<String> recipients, T payload) {
	}

	private final List<Sent<ChatMessagePayload>> messages = new ArrayList<>();
	private final List<Sent<ChatReadPayload>> reads = new ArrayList<>();
	private final List<Sent<ChatRoomOpenedPayload>> rooms = new ArrayList<>();

	public List<Sent<ChatMessagePayload>> messages() {
		return messages;
	}

	public List<Sent<ChatReadPayload>> reads() {
		return reads;
	}

	public List<Sent<ChatRoomOpenedPayload>> rooms() {
		return rooms;
	}

	@Override
	public void publishMessageCreated(List<String> recipientUsernames, ChatMessagePayload payload) {
		messages.add(new Sent<>(List.copyOf(recipientUsernames), payload));
	}

	@Override
	public void publishMessagesRead(String recipientUsername, ChatReadPayload payload) {
		reads.add(new Sent<>(List.of(recipientUsername), payload));
	}

	@Override
	public void publishRoomOpened(String recipientUsername, ChatRoomOpenedPayload payload) {
		rooms.add(new Sent<>(List.of(recipientUsername), payload));
	}
}
