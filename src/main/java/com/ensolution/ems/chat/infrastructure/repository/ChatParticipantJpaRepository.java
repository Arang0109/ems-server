package com.ensolution.ems.chat.infrastructure.repository;

import com.ensolution.ems.chat.infrastructure.entity.ChatParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatParticipantJpaRepository extends JpaRepository<ChatParticipantEntity, Long> {

	Optional<ChatParticipantEntity> findByRoomIdAndUserIdAndTenantId(Long roomId, Long userId, Long tenantId);

	List<ChatParticipantEntity> findAllByRoomIdAndTenantId(Long roomId, Long tenantId);

	List<ChatParticipantEntity> findAllByUserIdAndTenantId(Long userId, Long tenantId);

	List<ChatParticipantEntity> findAllByUserIdAndTenantIdAndHiddenFalse(Long userId, Long tenantId);
}
