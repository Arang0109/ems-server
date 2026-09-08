package com.ensolution.ems.chat.presentation.message.response;

import com.ensolution.ems.chat.domain.ChatMessageType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "메시지 한 건")
public record ChatMessageResponse(
	String messageId,
	Long roomId,
	Long senderId,
	String senderName,
	ChatMessageType type,
	String content,
	@Schema(description = "요청에 실려 온 값 그대로. 임시 말풍선을 치환하는 키")
	String clientMessageId,
	LocalDateTime sentAt
) {
}
