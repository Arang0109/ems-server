package com.ensolution.ems.chat.infrastructure.adapter;

import com.ensolution.ems.chat.application.command.UnreadCursor;
import com.ensolution.ems.chat.application.port.out.ChatMessageRepository;
import com.ensolution.ems.chat.domain.ChatMessage;
import com.ensolution.ems.chat.infrastructure.document.ChatMessageDocument;
import com.ensolution.ems.chat.infrastructure.mapper.ChatMessageDocumentMapper;
import com.ensolution.ems.chat.infrastructure.repository.ChatMessageMongoRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Limit;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryAdapter implements ChatMessageRepository {

	private final ChatMessageMongoRepository chatMessageMongoRepository;
	private final MongoTemplate mongoTemplate;
	private final ChatMessageDocumentMapper mapper;

	@Override
	public ChatMessage save(ChatMessage message) {
		return mapper.toDomain(chatMessageMongoRepository.save(mapper.toDocument(message)));
	}

	/**
	 * {@code roomId}까지 조건에 넣는다. 메시지 id만으로 찾으면 다른 방의 메시지를 자기 방의 것인 양
	 * 읽어 갈 수 있고, 호출자는 방의 참가자인지만 확인했을 뿐이다.
	 */
	@Override
	public ChatMessage findById(String messageId, Long roomId, Long tenantId) {
		return chatMessageMongoRepository.findByIdAndRoomIdAndTenantId(messageId, roomId, tenantId)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));
	}

	@Override
	public List<ChatMessage> findPage(Long roomId, Long tenantId, String before, int limit) {
		List<ChatMessageDocument> documents = before == null
			? chatMessageMongoRepository.findByTenantIdAndRoomIdOrderByIdDesc(tenantId, roomId, Limit.of(limit))
			: chatMessageMongoRepository.findByTenantIdAndRoomIdAndIdLessThanOrderByIdDesc(
				tenantId, roomId, before, Limit.of(limit));

		return mapper.toDomainList(documents);
	}

	/**
	 * 방별 미읽음 수를 <b>집계 한 번</b>으로 구한다.
	 * <p>
	 * 파생 메서드로는 방 수만큼 count 쿼리가 나가므로 목록 조회가 N+1이 된다. 그래서 이 하나만
	 * {@link MongoTemplate}을 직접 쓴다 — 방별로 커서가 다르다는 조건({@code $or})은 파생 메서드로
	 * 표현할 수 없다.
	 * <p>
	 * 커서가 {@code null}인 방(한 번도 읽지 않은 방)은 {@code _id} 조건 없이 방 전체를 센다.
	 */
	@Override
	public Map<Long, Long> countUnreadByRoom(Long tenantId, Long readerId, List<UnreadCursor> cursors) {
		if (cursors.isEmpty()) {
			return Map.of();
		}

		List<Criteria> perRoom = cursors.stream().map(ChatMessageRepositoryAdapter::criteriaOf).toList();

		Criteria criteria = Criteria.where("tenantId").is(tenantId)
			// 내가 보낸 메시지는 미읽음이 아니다.
			.and("senderId").ne(readerId)
			.orOperator(perRoom.toArray(Criteria[]::new));

		AggregationResults<UnreadCount> results = mongoTemplate.aggregate(
			Aggregation.newAggregation(
				Aggregation.match(criteria),
				Aggregation.group("roomId").count().as("count")
			),
			"chat_messages",
			UnreadCount.class
		);

		Map<Long, Long> countByRoomId = new HashMap<>();
		for (UnreadCount result : results) {
			countByRoomId.put(result.roomId(), result.count());
		}
		return countByRoomId;
	}

	private static Criteria criteriaOf(UnreadCursor cursor) {
		Criteria criteria = Criteria.where("roomId").is(cursor.roomId());
		if (cursor.lastReadMessageId() == null) {
			return criteria;
		}
		// _id 는 ObjectId 다. 문자열로 비교하면 타입이 달라 한 건도 걸리지 않는다.
		return criteria.and("_id").gt(new ObjectId(cursor.lastReadMessageId()));
	}

	/**
	 * {@code $group} 결과. {@code _id}에 담긴 그룹 키가 {@code roomId}다.
	 */
	private record UnreadCount(Long _id, long count) {

		Long roomId() {
			return _id;
		}
	}
}
