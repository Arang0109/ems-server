package com.ensolution.ems.chat.application.command;

import com.ensolution.ems.chat.domain.ChatMessageType;

import java.time.LocalDateTime;

/**
 * 메시지 한 건. 보낸 사람의 이름은 {@code auth}에서 조립해 채운다 —
 * 메시지 문서는 {@code senderId}만 갖는다.
 */
public record ChatMessageListItem(
	String messageId,
	Long roomId,
	Long senderId,
	String senderName,
	ChatMessageType type,
	String content,
	ChatAttachmentInfo attachment,
	String clientMessageId,
	LocalDateTime sentAt
) {
}
