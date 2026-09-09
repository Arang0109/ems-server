package com.ensolution.ems.chat.application;

import com.ensolution.ems.chat.application.port.out.ChatParticipantRepository;
import com.ensolution.ems.chat.domain.ChatParticipant;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 인메모리 {@link ChatParticipantRepository}.
 *
 * <p>참가자가 아닌 경우와 방이 없는 경우를 <b>구분하지 않고</b> 똑같이
 * {@code CHAT_ROOM_NOT_FOUND}를 던진다 — 어댑터와 같은 은닉 규약이다(루트 규칙 13).
 */
public class FakeChatParticipantRepository implements ChatParticipantRepository {

	private final List<ChatParticipant> participants = new ArrayList<>();
	private final AtomicLong sequence = new AtomicLong();

	public ChatParticipant given(Long tenantId, Long roomId, Long userId) {
		return save(ChatParticipant.join(tenantId, roomId, userId));
	}

	public int count() {
		return participants.size();
	}

	@Override
	public ChatParticipant save(ChatParticipant participant) {
		participants.removeIf(stored ->
			participant.getId() != null && Objects.equals(stored.getId(), participant.getId()));
		ChatParticipant saved = participant.getId() == null
			? participant.toBuilder().id(sequence.incrementAndGet()).build()
			: participant;
		participants.add(saved);
		return saved;
	}

	@Override
	public List<ChatParticipant> saveAll(List<ChatParticipant> toSave) {
		return toSave.stream().map(this::save).toList();
	}

	@Override
	public ChatParticipant findByRoomIdAndUserId(Long roomId, Long userId, Long tenantId) {
		return participants.stream()
			.filter(participant -> Objects.equals(participant.getRoomId(), roomId))
			.filter(participant -> Objects.equals(participant.getUserId(), userId))
			.filter(participant -> Objects.equals(participant.getTenantId(), tenantId))
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
	}

	@Override
	public List<ChatParticipant> findAllByRoomId(Long roomId, Long tenantId) {
		return participants.stream()
			.filter(participant -> Objects.equals(participant.getRoomId(), roomId))
			.filter(participant -> Objects.equals(participant.getTenantId(), tenantId))
			.toList();
	}

	@Override
	public List<ChatParticipant> findAllByRoomIds(List<Long> roomIds, Long tenantId) {
		return participants.stream()
			.filter(participant -> roomIds.contains(participant.getRoomId()))
			.filter(participant -> Objects.equals(participant.getTenantId(), tenantId))
			.toList();
	}

	@Override
	public List<ChatParticipant> findAllVisibleByUserId(Long userId, Long tenantId) {
		return findAllByUserId(userId, tenantId).stream()
			.filter(participant -> !participant.isHidden())
			.toList();
	}

	@Override
	public List<ChatParticipant> findAllByUserId(Long userId, Long tenantId) {
		return participants.stream()
			.filter(participant -> Objects.equals(participant.getUserId(), userId))
			.filter(participant -> Objects.equals(participant.getTenantId(), tenantId))
			.toList();
	}
}
