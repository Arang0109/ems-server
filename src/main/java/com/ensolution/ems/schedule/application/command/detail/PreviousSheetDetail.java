package com.ensolution.ems.schedule.application.command.detail;

import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;

import java.time.LocalDate;

/**
 * 새 기록지의 출발점으로 쓸 이전 회차 시트. 회차 고유값(시료번호·채취 시각·기상·버전)은 이미 비워져 있다.
 *
 * <p>출처를 함께 내려보내는 이유는 화면이 "언제 측정한 값을 가져왔는지" 보여줘야 하기 때문이다.
 * 어느 회차에서 왔는지 모르면 사용자가 그 값을 신뢰할지 판단할 수 없다.
 */
public record PreviousSheetDetail(
	Long sourceScheduleId,
	LocalDate sampledAt,
	String referenceNumber,
	MeasurementSheet sheet
) {}
