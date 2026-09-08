package com.ensolution.ems.chat.infrastructure.adapter;

import com.ensolution.ems.chat.application.port.out.ChatParticipantRepository;
import com.ensolution.ems.chat.domain.ChatParticipant;
import com.ensolution.ems.chat.infrastructure.mapper.ChatParticipantEntityMapper;
import com.ensolution.ems.chat.infrastructure.repository.ChatParticipantJpaRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional
public class ChatParticipantRepositoryAdapter implements ChatParticipantRepository {

	private final ChatParticipantJpaRepository chatParticipantJpaRepository;
	private final ChatParticipantEntityMapper mapper;

	@Override
	public ChatParticipant save(ChatParticipant participant) {
		return mapper.toDomain(chatParticipantJpaRepository.save(mapper.toEntity(participant)));
	}

	@Override
	public List<ChatParticipant> saveAll(List<ChatParticipant> participants) {
		return mapper.toDomainList(chatParticipantJpaRepository.saveAll(
			participants.stream().map(mapper::toEntity).toList()
		));
	}

	/**
	 * <b>참가자가 아닌 것과 방이 없는 것을 구분하지 않는다.</b> 둘 다
	 * {@code CHAT_ROOM_NOT_FOUND}로 답해야 남의 방의 존재가 드러나지 않는다(루트 규칙 13).
	 */
	@Override
	@Transactional(readOnly = true)
	public ChatParticipant findByRoomIdAndUserId(Long roomId, Long userId, Long tenantId) {
		return chatParticipantJpaRepository.findByRoomIdAndUserIdAndTenantId(roomId, userId, tenantId)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ChatParticipant> findAllByRoomId(Long roomId, Long tenantId) {
		return mapper.toDomainList(chatParticipantJpaRepository.findAllByRoomIdAndTenantId(roomId, tenantId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ChatParticipant> findAllVisibleByUserId(Long userId, Long tenantId) {
		return mapper.toDomainList(
			chatParticipantJpaRepository.findAllByUserIdAndTenantIdAndHiddenFalse(userId, tenantId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ChatParticipant> findAllByUserId(Long userId, Long tenantId) {
		return mapper.toDomainList(chatParticipantJpaRepository.findAllByUserIdAndTenantId(userId, tenantId));
	}
}
