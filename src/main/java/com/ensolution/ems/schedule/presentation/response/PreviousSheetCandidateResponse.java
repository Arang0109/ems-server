package com.ensolution.ems.schedule.presentation.response;

import java.time.LocalDate;

/**
 * 이전 기록 불러오기 출처 후보 응답. 측정일 내림차순이며 목록이 비면 불러올 기록이 없다는 뜻이다.
 * 시트 본문은 사용자가 후보를 고른 뒤 {@code GET .../previous?sourceScheduleId=} 로 받는다.
 */
public record PreviousSheetCandidateResponse(
	Long sourceScheduleId,
	LocalDate sampledAt,
	String referenceNumber
) {}
