package com.ensolution.ems.client_management.domain;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class StackPollutant {
	private Long id;
	private Long tenantId;
	private Long stackId;
	private Long pollutantId;
	private MeasurementCycle cycle;
	private BigDecimal allowance;
	private boolean oxygenApplicable;

	public static StackPollutant register(Long tenantId, Long stackId, Long pollutantId, MeasurementCycle cycle, BigDecimal allowance, boolean oxygenApplicable) {
		return StackPollutant.builder()
			.tenantId(tenantId)
			.stackId(stackId)
			.pollutantId(pollutantId)
			.cycle(cycle)
			.allowance(allowance)
			.oxygenApplicable(oxygenApplicable)
			.build();
	}

	/**
	 * 측정 조건만 수정한다. {@code stackId}·{@code pollutantId}는 대상이 아니다 —
	 * 어떤 시설의 어떤 물질인지가 바뀌면 다른 항목이지 수정이 아니다.
	 *
	 * <p>허용기준은 nullable 이므로 받은 값을 그대로 반영한다. null을 "유지"로 읽으면
	 * 한번 채운 허용기준을 다시 비울 방법이 없어져 "미지정"과 "0"을 구분할 수 없다.
	 */
	public StackPollutant update(MeasurementCycle cycle, BigDecimal allowance, boolean oxygenApplicable) {
		return this.toBuilder()
			.cycle(cycle == null ? this.cycle : cycle)
			.allowance(allowance)
			.oxygenApplicable(oxygenApplicable)
			.build();
	}
}