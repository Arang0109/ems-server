package com.ensolution.ems.chat.application.command;

/**
 * 방 하나의 미읽음 기준점. 방 목록의 미읽음 수를 <b>집계 한 번</b>으로 구하기 위해
 * 여러 방의 커서를 모아 저장소에 넘긴다.
 *
 * @param lastReadMessageId {@code null}이면 한 번도 읽지 않은 방이다 — 그 방의 메시지 전부가 미읽음이다
 */
public record UnreadCursor(
	Long roomId,
	String lastReadMessageId
) {
}
