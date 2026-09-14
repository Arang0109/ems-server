package com.ensolution.ems.client_management.application.command.update;

import com.ensolution.ems.global.common.enums.SampleGrouping;

/**
 * {@code name}·{@code sampleGrouping}은 null이면 유지, {@code mergedSampleName}·{@code samplingMinutes}는
 * 전달값을 그대로 채택한다(null = 비움). 근거는 {@code MeasurementMethod#update} javadoc.
 */
public record UpdateMeasurementMethodCommand(
	String name,
	SampleGrouping sampleGrouping,
	String mergedSampleName,
	Integer samplingMinutes
) {}
