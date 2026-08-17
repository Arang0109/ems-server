package com.ensolution.ems.client_management.presentation.stack_pollutant.request;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * @param pollutantId 이미 이 고객사에 등록된 측정물질 id
 * @param catalogId   전역 카탈로그 id. 아직 등록하지 않은 카탈로그 물질을 고를 때 사용하며,
 *                    서버가 이 고객사의 측정물질을 자동으로 만들어 연결한다.
 *                    (code가 아니라 id를 쓰는 이유: code는 측정분야 안에서만 유일하다)
 */
public record CreateStackPollutantRequest(
	@NotNull
	Long stackId,
	Long pollutantId,
	Long catalogId,
	@NotNull
	MeasurementCycle cycle,
	BigDecimal allowance,
	boolean oxygenApplicable
	) {

	@JsonIgnore
	@AssertTrue(message = "pollutantId 또는 catalogId 중 하나는 필수입니다.")
	public boolean isPollutantReferencePresent() {
		return pollutantId != null || catalogId != null;
	}
}
