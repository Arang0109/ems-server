package com.ensolution.ems.schedule.application.command.detail;

import java.time.LocalDate;

/**
 * 이전 기록 불러오기의 출처 후보. 해당 기록지를 실제로 쓴 완료 회차 하나를 가리킨다.
 *
 * <p>시트 본문은 담지 않는다 — 후보는 여러 건이고 시트는 무거우므로, 목록은 "어느 회차인가"만
 * 보여주고 실제 값은 사용자가 고른 뒤 {@link PreviousSheetDetail} 로 따로 조회한다.
 */
public record PreviousSheetCandidate(
	Long sourceScheduleId,
	LocalDate sampledAt,
	String referenceNumber
) {}
