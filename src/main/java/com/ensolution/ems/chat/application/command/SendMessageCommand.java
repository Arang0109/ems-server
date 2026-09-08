package com.ensolution.ems.chat.application.command;

/**
 * 메시지 전송 요청.
 *
 * @param clientMessageId 클라이언트가 만든 UUID. 서버는 해석하지 않고 그대로 돌려준다
 *                        (낙관적 UI 의 임시 말풍선을 치환하는 키)
 */
public record SendMessageCommand(
	Long tenantId,
	Long roomId,
	Long senderId,
	String content,
	String clientMessageId,

	/** 첨부가 없으면 null. 텍스트 전송과 첨부 전송이 같은 유스케이스로 들어오게 하는 필드다 */
	AttachmentUpload attachment
) {
}
