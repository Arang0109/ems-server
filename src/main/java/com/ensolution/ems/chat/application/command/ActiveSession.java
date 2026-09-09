package com.ensolution.ems.chat.application.command;

/**
 * 이 인스턴스에 붙어 있는 WebSocket 세션 하나. 주기 갱신이 대상을 넘기는 형태다.
 * <p>
 * 세션 목록은 <b>프로세스 로컬</b>이다 — 그래서 프레즌스는 Redis 에 있어도 갱신은 각 인스턴스가
 * 자기 세션에 대해서만 한다.
 */
public record ActiveSession(
	Long tenantId,
	Long userId,
	String sessionId
) {
}
