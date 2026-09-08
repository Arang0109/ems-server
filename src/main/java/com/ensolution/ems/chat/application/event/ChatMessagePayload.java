package com.ensolution.ems.chat.application.event;

import com.ensolution.ems.chat.application.command.ChatAttachmentInfo;
import com.ensolution.ems.chat.domain.ChatMessageType;

import java.time.LocalDateTime;

/**
 * 새 메시지 알림.
 * <p>
 * <b>{@code tenantId}를 담지 않습니다.</b> 수신자를 정하는 데만 쓰이는 서버 내부 값이고,
 * 목적지가 이미 사용자별 큐라 클라이언트가 그것으로 무엇을 걸러 낼 일이 없습니다.
 * <p>
 * SSE 의 {@code SheetsSavedEvent}는 "무엇이 바뀌었는지"만 알리고 본문은 재조회하게 했지만,
 * 채팅은 <b>본문을 그대로 싣습니다</b>. 메시지마다 목록 재조회를 유발하면 왕복이 두 배가 되고,
 * 페이로드가 이미 화면에 그릴 것 전부이기 때문입니다.
 */
public record ChatMessagePayload(
	Long roomId,
	String messageId,
	Long senderId,
	String senderName,
	ChatMessageType type,
	String content,
	ChatAttachmentInfo attachment,
	String clientMessageId,
	LocalDateTime sentAt
) {
}
