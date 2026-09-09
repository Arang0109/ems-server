package com.ensolution.ems.chat.application;

import com.ensolution.ems.chat.application.port.out.ChatRoomRepository;
import com.ensolution.ems.chat.domain.ChatRoom;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 인메모리 {@link ChatRoomRepository}.
 *
 * <p><b>{@code (tenantId, pairKey)} 유니크를 실제 DB 제약처럼 재현한다.</b> 이 Fake가 중복 저장을
 * 허용해 버리면 "같은 두 사람에게 방은 하나"를 검증하는 테스트가 통과해도 아무것도 보증하지 못한다.
 * 실제 어댑터와 마찬가지로 {@link DataIntegrityViolationException}을 그대로 던져,
 * 충돌을 재조회로 흡수하는 책임이 {@code DirectRoomWriter}에 있다는 것을 구조로 고정한다.
 *
 * <p>tenant 필터도 어댑터의 WHERE 절 그대로다 — 무시하면 격리 테스트가 무의미해진다.
 */
public class FakeChatRoomRepository implements ChatRoomRepository {

	private final List<ChatRoom> rooms = new ArrayList<>();
	private final AtomicLong sequence = new AtomicLong();

	/** 다음 저장에서 유니크 충돌을 일으킨다. 동시 개설 경쟁을 결정적으로 재현하기 위한 장치다. */
	private boolean failNextSaveWithConflict;

	public ChatRoom given(Long tenantId, Long userA, Long userB) {
		return save(ChatRoom.openDirect(tenantId, userA, userB));
	}

	public void failNextSaveWithConflict() {
		this.failNextSaveWithConflict = true;
	}

	public int count() {
		return rooms.size();
	}

	@Override
	public ChatRoom save(ChatRoom room) {
		if (failNextSaveWithConflict) {
			failNextSaveWithConflict = false;
			throw new DataIntegrityViolationException("uk_chat_rooms_tenant_pair");
		}

		if (room.getId() == null) {
			boolean duplicated = rooms.stream().anyMatch(stored ->
				Objects.equals(stored.getTenantId(), room.getTenantId())
					&& Objects.equals(stored.getPairKey(), room.getPairKey()));
			if (duplicated) {
				throw new DataIntegrityViolationException("uk_chat_rooms_tenant_pair");
			}
		}

		rooms.removeIf(stored -> room.getId() != null && Objects.equals(stored.getId(), room.getId()));
		ChatRoom saved = room.getId() == null
			? room.toBuilder().id(sequence.incrementAndGet()).build()
			: room;
		rooms.add(saved);
		return saved;
	}

	@Override
	public ChatRoom findById(Long roomId, Long tenantId) {
		return rooms.stream()
			.filter(room -> Objects.equals(room.getId(), roomId))
			.filter(room -> Objects.equals(room.getTenantId(), tenantId))
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
	}

	@Override
	public Optional<ChatRoom> findByPairKey(String pairKey, Long tenantId) {
		return rooms.stream()
			.filter(room -> Objects.equals(room.getPairKey(), pairKey))
			.filter(room -> Objects.equals(room.getTenantId(), tenantId))
			.findFirst();
	}

	@Override
	public List<ChatRoom> findAllByIds(List<Long> roomIds, Long tenantId) {
		return rooms.stream()
			.filter(room -> roomIds.contains(room.getId()))
			.filter(room -> Objects.equals(room.getTenantId(), tenantId))
			.toList();
	}
}
