package com.ensolution.ems.chat.infrastructure.adapter;

import com.ensolution.ems.chat.application.port.out.ChatRoomRepository;
import com.ensolution.ems.chat.domain.ChatRoom;
import com.ensolution.ems.chat.infrastructure.mapper.ChatRoomEntityMapper;
import com.ensolution.ems.chat.infrastructure.repository.ChatRoomJpaRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional
public class ChatRoomRepositoryAdapter implements ChatRoomRepository {

	private final ChatRoomJpaRepository chatRoomJpaRepository;
	private final ChatRoomEntityMapper mapper;

	/**
	 * <b>유니크 충돌을 여기서 삼키지 않는다.</b> {@code DataIntegrityViolationException}이 그대로
	 * 올라가야 {@code DirectRoomWriter}가 "다른 요청이 방금 만들었다"로 해석해 재조회할 수 있다.
	 * 여기서 잡아 null이나 기존 방을 돌려주면 그 판단이 어댑터로 새어 든다.
	 * <p>
	 * 제약 위반을 즉시 드러내려면 flush가 필요하다 — 트랜잭션 커밋까지 미루면 예외가
	 * {@code DirectRoomWriter}의 try 밖에서 터진다.
	 */
	@Override
	public ChatRoom save(ChatRoom room) {
		return mapper.toDomain(chatRoomJpaRepository.saveAndFlush(mapper.toEntity(room)));
	}

	@Override
	public ChatRoom findById(Long roomId, Long tenantId) {
		return chatRoomJpaRepository.findByRoomIdAndTenantId(roomId, tenantId)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ChatRoom> findByPairKey(String pairKey, Long tenantId) {
		return chatRoomJpaRepository.findByPairKeyAndTenantId(pairKey, tenantId).map(mapper::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ChatRoom> findAllByIds(List<Long> roomIds, Long tenantId) {
		if (roomIds.isEmpty()) {
			return List.of();
		}
		return mapper.toDomainList(chatRoomJpaRepository.findAllByRoomIdInAndTenantId(roomIds, tenantId));
	}
}
