package com.ensolution.ems.chat.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 대화방 참가자. 1:1이므로 방마다 두 건이다.
 * <p>
 * <b>{@code ChatRoomEntity}와 JPA 연관을 두지 않는다.</b> 같은 모듈이라 걸어도 규칙 위반은 아니지만,
 * 조회 방향이 언제나 <b>참가자 → 방</b>("내 대화방 목록")이라 방에서 참가자를 객체 그래프로 끌 일이
 * 없다. {@code ScheduleEntity}가 연관관계를 하나도 두지 않은 것과 같은 판단이다.
 * <p>
 * {@code user_id}는 {@code auth}의 것이므로 반드시 plain {@code Long}이다. 이름이 필요하면
 * {@code UserQueryUseCase}로 가져온다.
 */
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Table(
	name = "chat_room_participants",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_chat_participants_room_user", columnNames = {"room_id", "user_id"}
	),
	indexes = @Index(
		name = "idx_chat_participants_tenant_user", columnList = "tenant_id, user_id"
	)
)
public class ChatParticipantEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "participant_id")
	private Long participantId;

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "room_id", nullable = false)
	private Long roomId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	/** Mongo {@code ObjectId} 문자열. 미읽음은 이 커서 뒤를 세서 구한다 */
	@Column(name = "last_read_message_id", length = 24)
	private String lastReadMessageId;

	@Column(name = "last_read_at")
	private LocalDateTime lastReadAt;

	/** 내 목록에서만 감춘 상태. 상대의 대화는 그대로다 */
	@Column(name = "hidden", nullable = false)
	private boolean hidden;

	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "modified_at")
	private LocalDateTime modifiedAt;
}
