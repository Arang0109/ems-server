package com.ensolution.ems.client_management.application.port.in;

import com.ensolution.ems.global.common.enums.Grade;
import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMode;
import com.ensolution.ems.global.common.enums.Orientation;
import com.ensolution.ems.global.common.enums.PollutantPhase;
import com.ensolution.ems.global.common.enums.SampleGrouping;
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

	/**
	 * @param code   측정물질 가이드 키(예: {@code NOX}). 모든 tenant에서 동일하므로 물질 판별에 쓴다.
	 *               고객사는 가이드 밖의 물질을 만들 수 없으므로 항상 채워진다
	 * @param mode   측정방식 분류(카탈로그 전역 사실). 회사 측정방법과 무관하게 항목을 묶는 축
	 * @param method 이 고객사가 이 물질에 쓰는 측정방법의 사본. 측정방법이 정해지지 않은 레거시 행은 null
	 * @param samplingMinutes 이 항목에 적용되는 표준 채취시간(분). 항목 오버라이드가 있으면 그것, 없으면 방법 기본값.
	 *                        {@code method.samplingMinutes}는 방법의 기본값이므로 항목 값은 이것을 본다
	 */
	public record MeasurementItemInfo(
		Long stackPollutantId,
		Long pollutantId,
		String code,
		String nameKr,
		String nameEn,
		MeasurementField field,
		MeasurementMethodInfo method,
		PollutantPhase phase,
		MeasurementMode mode,
		String equipment,
		String testMethod,
		Integer samplingMinutes,
		MeasurementCycle cycle,
		BigDecimal allowance,
		boolean oxygenApplicable
	) {}

	/**
	 * 측정방법 요약. 채취 단위와 통칭 시료명은 현장 기록지가 가스상 시료 행을 만드는 근거이고,
	 * 표준 채취시간은 계획 기본값이다. 소비 모듈은 이것을 사본으로 저장한다.
	 *
	 * @param mergedSampleName {@link SampleGrouping#MERGED}일 때만 값이 있다
	 * @param samplingMinutes  표준 채취시간(분). 미지정이면 null
	 */
	public record MeasurementMethodInfo(
		Long methodId,
		String name,
		SampleGrouping sampleGrouping,
		String mergedSampleName,
		Integer samplingMinutes
	) {}
}
