package com.ensolution.ems.chat.application.command;

import java.util.List;

/**
 * 커서 페이지. <b>offset 페이징을 쓰지 않는 이유</b>는 읽는 동안 새 메시지가 앞에 붙어
 * offset 이 밀리기 때문이다 — 위로 스크롤할수록 이미 본 메시지가 다시 나오거나 건너뛴다.
 *
 * @param messages   최신순
 * @param nextCursor 다음 페이지를 요청할 때 {@code before}로 넘길 값. 더 없으면 {@code null}
 */
public record ChatMessagePage(
	List<ChatMessageListItem> messages,
	String nextCursor,
	boolean hasMore
) {
}
