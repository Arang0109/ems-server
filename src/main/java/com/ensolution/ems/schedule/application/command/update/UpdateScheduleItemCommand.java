package com.ensolution.ems.schedule.application.command.update;

import com.ensolution.ems.global.common.enums.MeasurementCycle;

import java.math.BigDecimal;

/**
 * 이번 측정계획에 담긴 측정항목 하나의 측정 조건 정정 커맨드.
 * 어느 물질인지는 경로({@code pollutantId})가 정하므로 대상이 아니다.
 *
 * <p>{@code allowance}는 null이 "미지정"이라 전달값이 그대로 반영된다.
 */
public record UpdateScheduleItemCommand(
	MeasurementCycle cycle,
	BigDecimal allowance,
	boolean oxygenApplicable
) {}
