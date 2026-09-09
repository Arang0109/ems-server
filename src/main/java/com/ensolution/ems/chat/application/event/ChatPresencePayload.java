package com.ensolution.ems.chat.application.event;

import com.ensolution.ems.chat.domain.PresenceStatus;

import java.time.LocalDateTime;

/** 누군가의 접속 상태가 바뀌었다는 알림. 연락처 목록과 대화방 헤더의 점을 켜고 끈다. */
public record ChatPresencePayload(
	Long userId,
	PresenceStatus status,
	LocalDateTime changedAt
) {
}
