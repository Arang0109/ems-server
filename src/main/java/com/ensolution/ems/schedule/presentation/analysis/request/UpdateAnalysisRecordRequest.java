package com.ensolution.ems.schedule.presentation.analysis.request;

import java.math.BigDecimal;

/** 전달하지 않은 필드는 기존 값을 유지한다. */
public record UpdateAnalysisRecordRequest(
	BigDecimal analysisValue,
	String unit,
	String analysisMethod,
	String analysisEquipment
) {}
