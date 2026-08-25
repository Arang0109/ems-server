package com.ensolution.ems.schedule.application.command.update;

import java.math.BigDecimal;

/** 실험분석정보 수정 파라미터. 전달되지 않은 필드는 기존 값을 유지한다. */
public record UpdateAnalysisRecordCommand(
	BigDecimal analysisValue,
	String unit,
	String analysisMethod,
	String analysisEquipment
) {}
