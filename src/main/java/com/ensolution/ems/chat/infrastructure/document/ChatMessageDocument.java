package com.ensolution.ems.chat.infrastructure.document;

import com.ensolution.ems.chat.domain.ChatAttachment;
import com.ensolution.ems.chat.domain.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 대화 한 건. 방·참가자는 MySQL 원장이고 메시지만 MongoDB에 쌓입니다 — 대량 append-only이고
 * 첨부처럼 뒤에 붙을 구조가 유동적이기 때문입니다({@code equipment_inspection_records}와 같은 성격).
 * <p>
 * <b>인덱스를 하나만 둡니다.</b> 대화 페이지 조회({@code tenantId}+{@code roomId} 최신순)와
 * 미읽음 집계({@code _id > 커서}) 둘 다 이 복합 인덱스로 커버됩니다. {@code senderId}를 넣은
 * 두 번째 인덱스는 집계에서 걸러 낼 뿐이라 쓰기 비용만 늘립니다.
 * <p>
 * {@code _id}는 {@code ObjectId}입니다. 도메인은 이것을 16진 문자열로 다루며, 그 문자열의
 * 사전순이 곧 생성순이라 커서로 쓸 수 있습니다.
 */
@Document("chat_messages")
@CompoundIndex(
	name = "idx_chat_messages_tenant_room_id",
	def = "{'tenantId': 1, 'roomId': 1, '_id': -1}"
)
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDocument {

	@Id
	private String id;

	@Indexed
	private Long tenantId;

	private Long roomId;

	/** {@code auth}의 user_id. 모듈 경계를 넘으므로 값으로만 갖는다 */
	private Long senderId;

	private ChatMessageType type;

	/** 첨부만 있는 메시지는 비어 있을 수 있다 */
	private String content;

	/** 첨부가 없으면 null. 도메인 {@code ChatAttachment}를 그대로 임베드한다 */
	private ChatAttachment attachment;

	/** 클라이언트가 만든 UUID. 서버는 해석하지 않고 되돌려 주기만 한다 */
	private String clientMessageId;

	@CreatedDate
	private LocalDateTime sentAt;
}
