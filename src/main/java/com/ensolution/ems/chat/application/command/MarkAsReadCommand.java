package com.ensolution.ems.chat.application.command;

/**
 * 읽음 보고.
 *
 * @param lastReadMessageId 여기까지 읽었다는 메시지 id. 커서는 앞으로만 가므로
 *                          이보다 앞선 값이 오면 무시된다
 */
public record MarkAsReadCommand(
	Long tenantId,
	Long roomId,
	Long readerId,
	String lastReadMessageId
) {
}
