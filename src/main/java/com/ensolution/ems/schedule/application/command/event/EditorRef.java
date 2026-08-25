package com.ensolution.ems.schedule.application.command.event;

/**
 * 편집을 일으킨 사용자. 실시간 알림에만 쓰인다.
 * <p>
 * {@code username}(로그인 id)을 싣는 이유는 수신 측이 <b>자기 저장으로 돌아온 메아리</b>를
 * 걸러내야 하기 때문이다 — 클라이언트가 로그인 시점부터 들고 있는 식별자가 이것뿐이다.
 * {@code name}은 "OOO님이 배출가스를 수정했습니다" 안내에 쓴다.
 */
public record EditorRef(
	String username,
	String name
) {}
