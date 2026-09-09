package com.ensolution.ems.chat.application.event;

import java.time.LocalDateTime;

/**
 * 상대가 어디까지 읽었다는 알림. 내가 보낸 말풍선의 "읽음" 표시를 지우거나 붙이는 데 쓴다.
 * <p>
 * 읽은 사람 자신에게는 보내지 않는다 — 자기 화면은 이미 알고 있다.
 */
public record ChatReadPayload(
	Long roomId,
	Long readerId,
	String lastReadMessageId,
	LocalDateTime readAt
) {
}
