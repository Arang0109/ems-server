package com.ensolution.ems.schedule.presentation.request;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 측정계획에 담긴 측정항목 하나의 측정 조건 정정 요청.
 * 어느 물질인지는 경로가 정하므로 담지 않는다.
 */
@Schema(description = "측정계획 측정항목 정정 요청. 이 회차 문서의 측정 조건(측정주기·허용기준·산소보정)만 바로잡습니다. "
	+ "측정시설 원장(stack_pollutant)은 변경되지 않으므로, 원장도 함께 고치려면 "
	+ "PUT /api/stack-pollutants/{id} 를 따로 호출해야 합니다.")
public record UpdateScheduleItemRequest(
	@Schema(description = "측정주기")
	@NotNull(message = "측정주기는 필수입니다.")
	MeasurementCycle cycle,

	@Schema(description = "배출허용기준. null이면 '미지정'으로 비웁니다(0과 구분됩니다)")
	BigDecimal allowance,

	@Schema(description = "측정시설의 기준산소농도를 이 항목에 적용할지 여부")
	boolean oxygenApplicable
) {}
