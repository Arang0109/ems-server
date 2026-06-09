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
	private Long stackId;
	private Long pollutantId;
	private MeasurementCycle cycle;
	private BigDecimal allowance;

	public static StackPollutant register(Long stackId, Long pollutantId, MeasurementCycle cycle, BigDecimal allowance) {
		return StackPollutant.builder()
			.stackId(stackId)
			.pollutantId(pollutantId)
			.cycle(cycle)
			.allowance(allowance)
			.build();
	}
}
