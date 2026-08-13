package com.ensolution.ems.client_management.application.port.in;

import com.ensolution.ems.global.common.enums.Grade;
import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.Orientation;
import com.ensolution.ems.global.common.enums.PollutantPhase;
import com.ensolution.ems.global.common.enums.Shape;

import java.math.BigDecimal;
import java.util.List;

/**
 * 타 모듈(schedule 등)이 측정 시점 대상(의뢰기관·사업장·측정시설·설비·측정항목) 스냅샷을 구성할 때
 * 사용하는 측정 대상 요약 트리 VO. tenant 모듈의 외부 공개 계약이므로 {@code application/port/in}에 위치한다.
 */
public record StackMeasurementSummary(
	ClientInfo client,
	WorkplaceInfo workplace,
	StackInfo stack,
	List<FacilityInfo> facilities,
	List<PreventionInfo> preventions,
	List<MeasurementItemInfo> measurementItems
) {
	public record ClientInfo(
		Long clientId,
		String name,
		String bizNumber,
		String representative,
		String roadAddress,
		String detailAddress,
		String zipcode,
		String email,
		String tel
	) {}

	public record WorkplaceInfo(
		Long workplaceId,
		String name,
		String bizNumber,
		String businessCategory,
		String roadAddress,
		String detailAddress,
		String zipcode,
		String facilityManager,
		String samplingWitness,
		Grade grade
	) {}

	public record StackInfo(
		Long stackId,
		MeasurementField field,
		String name,
		String semsNumber,
		Grade grade,
		String mainProduct,
		Integer standardOxygen,
		Double height,
		Double horizontalLength,
		Double verticalLength,
		Shape shape,
		Orientation orientation
	) {}

	public record FacilityInfo(
		Long facilityId,
		String name,
		String fuelUsage,
		String productOutput,
		String incinerationAmount,
		String fuelInput,
		String fuelType,
		String unit
	) {}

	public record PreventionInfo(
		Long preventionId,
		String name,
		Double capacity,
		String unit,
		String targetName,
		String removalEfficiency
	) {}

	public record MeasurementItemInfo(
		Long stackPollutantId,
		Long pollutantId,
		String nameKr,
		String nameEn,
		MeasurementField field,
		MeasurementMethod method,
		PollutantPhase phase,
		String equipment,
		String testMethod,
		MeasurementCycle cycle,
		BigDecimal allowance,
		boolean oxygenApplicable
	) {}
}
