package com.ensolution.ems.chat.infrastructure.repository;

import com.ensolution.ems.chat.infrastructure.document.ChatMessageDocument;
import org.springframework.data.domain.Limit;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageMongoRepository extends MongoRepository<ChatMessageDocument, String> {

	Optional<ChatMessageDocument> findByIdAndRoomIdAndTenantId(String id, Long roomId, Long tenantId);

	/** 첫 페이지. 가장 최근부터 {@code limit}건. */
	List<ChatMessageDocument> findByTenantIdAndRoomIdOrderByIdDesc(Long tenantId, Long roomId, Limit limit);

	/** 이어지는 페이지. {@code before}보다 앞선 것들 중 최신 {@code limit}건. */
	List<ChatMessageDocument> findByTenantIdAndRoomIdAndIdLessThanOrderByIdDesc(
		Long tenantId, Long roomId, String before, Limit limit);
}
