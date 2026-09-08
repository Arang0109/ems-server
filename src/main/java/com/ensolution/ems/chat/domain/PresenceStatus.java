package com.ensolution.ems.chat.domain;

/** 접속 상태. 진실의 원천은 살아 있는 WebSocket 세션이며, DB 컬럼이 아니다. */
public enum PresenceStatus {

	ONLINE,
	OFFLINE
}
