package com.ensolution.ems.schedule.application.command.update;

import java.time.LocalDate;

/**
 * 성적서를 진행하며 채우는 일자 셋의 수정 커맨드.
 *
 * <p>실험·분석 탭이 이 셋을 <b>단독으로 소유</b>하므로 <b>전체 채택</b>이다 — 전달되지 않은(null) 칸은
 * "지웠다"는 뜻이며, 그래서 잘못 넣은 일자를 비울 수 있다. 계획을 정의하는 값(채취일자·측정용도·
 * 관리번호)은 이 커맨드의 대상이 아니다({@link UpdateScheduleCommand} 경로를 쓴다) — 소유 화면이
 * 다르므로 한 경로에 묶으면 서로의 빈 칸이 상대의 값을 지운다.
 */
public record UpdateReportDatesCommand(
	LocalDate receivedAt,
	LocalDate analyzedAt,
	LocalDate issuedAt
) {}
