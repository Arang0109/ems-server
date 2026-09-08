package com.ensolution.ems.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 대화방에 속한 한 사람. 1:1이므로 방마다 정확히 두 건입니다.
 * <p>
 * 읽음 위치({@code lastReadMessageId})가 여기 있는 이유는 그것이 사용자의 속성이 아니라
 * <b>(사용자, 방) 쌍의 속성</b>이기 때문입니다. {@code users}에는 둘 자리가 없습니다.
 * <p>
 * {@code hidden}은 "나가기"입니다. 1:1 대화에서 한쪽이 나간다고 상대의 대화까지 사라지면 안 되므로,
 * 방을 지우는 것이 아니라 <b>내 목록에서만 감춥니다</b>. 상대가 새 메시지를 보내면 다시 나타납니다.
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ChatParticipant {

	private Long id;
	private Long tenantId;
	private Long roomId;
	private Long userId;
	private String lastReadMessageId;
	private LocalDateTime lastReadAt;
	private boolean hidden;

	public static ChatParticipant join(Long tenantId, Long roomId, Long userId) {
		return ChatParticipant.builder()
			.tenantId(tenantId)
			.roomId(roomId)
			.userId(userId)
			.hidden(false)
			.build();
	}

	/**
	 * 읽음 위치를 앞으로만 옮긴다.
	 * <p>
	 * <b>커서는 되돌아가지 않는다.</b> 위로 스크롤해 옛 메시지를 보는 것은 읽음 취소가 아니고,
	 * 프레임 도착 순서가 뒤바뀌면 오래된 커서가 뒤늦게 도착할 수도 있다. 두 경우 모두
	 * 이미 읽은 메시지가 다시 안 읽음으로 돌아가서는 안 된다.
	 * <p>
	 * 비교는 Mongo {@code ObjectId} 문자열의 사전순이다. ObjectId는 앞 4바이트가 초 단위
	 * 타임스탬프이고 뒤이어 단조 증가 카운터가 오므로, 16진 문자열의 사전순이 곧 생성순이다.
	 * <p>
	 * <b>그래서 형태부터 확인한다.</b> 사전순 비교는 ObjectId 끼리일 때만 생성순을 뜻하고, 임의
	 * 문자열이 섞이면 비교가 무의미해진다. 더 나쁜 것은 그런 값이 저장된 뒤다 — 이 커서는 다음
	 * 미읽음 집계의 쿼리 인자가 되므로 그때부터 방 목록 조회가 매번 터지고, 임의 문자열은 대개
	 * 실제 ObjectId 보다 사전순으로 커서 <b>앞으로만 가는 커서가 영영 덮어쓰지 못한다.</b>
	 */
	public ChatParticipant readUpTo(String messageId, LocalDateTime readAt) {
		if (messageId == null) {
			return this;
		}
		ChatMessage.requireValidId(messageId);

		if (lastReadMessageId != null && messageId.compareTo(lastReadMessageId) <= 0) {
			return this;
		}
		return this.toBuilder()
			.lastReadMessageId(messageId)
			.lastReadAt(readAt)
			.build();
	}

	/** 내 목록에서 감춘다. 상대의 대화는 그대로 남는다. */
	public ChatParticipant hide() {
		return this.toBuilder().hidden(true).build();
	}

	/** 새 메시지가 오면 감춰 둔 방이 다시 나타난다. */
	public ChatParticipant reveal() {
		return hidden ? this.toBuilder().hidden(false).build() : this;
	}
}
