package com.ensolution.ems.chat.infrastructure.repository;

import com.ensolution.ems.chat.infrastructure.entity.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomJpaRepository extends JpaRepository<ChatRoomEntity, Long> {

	Optional<ChatRoomEntity> findByRoomIdAndTenantId(Long roomId, Long tenantId);

	Optional<ChatRoomEntity> findByPairKeyAndTenantId(String pairKey, Long tenantId);

	List<ChatRoomEntity> findAllByRoomIdInAndTenantId(List<Long> roomIds, Long tenantId);
}
