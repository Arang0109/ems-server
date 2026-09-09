package com.ensolution.ems.chat.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 1:1 대화방. {@code tenant_id}는 {@code users}·{@code contracts}와 같은 <b>plain 컬럼</b>이다 —
 * 모듈 경계를 넘는 JPA 연관을 두지 않는다.
 * <p>
 * <b>{@code uk_chat_rooms_tenant_pair}가 이 테이블의 존재 이유 절반이다.</b> 두 사람이 동시에
 * 대화를 시작해도 방이 하나만 남게 하는 것은 이 제약뿐이며, 애플리케이션 검사로는 막을 수 없다.
 */
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Table(
	name = "chat_rooms",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_chat_rooms_tenant_pair", columnNames = {"tenant_id", "pair_key"}
	),
	indexes = @Index(
		name = "idx_chat_rooms_tenant_last_message", columnList = "tenant_id, last_message_at"
	)
)
public class ChatRoomEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "room_id")
	private Long roomId;

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	/** {@code min(userId):max(userId)}. 도메인이 만든다 */
	@Column(name = "pair_key", nullable = false, length = 64)
	private String pairKey;

	/** 목록 정렬·미리보기용 비정규화 사본. 본문의 진실은 MongoDB {@code chat_messages}에 있다 */
	@Column(name = "last_message_id", length = 24)
	private String lastMessageId;

	@Column(name = "last_message_preview", length = 200)
	private String lastMessagePreview;

	@Column(name = "last_message_at")
	private LocalDateTime lastMessageAt;

	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "modified_at")
	private LocalDateTime modifiedAt;
}
