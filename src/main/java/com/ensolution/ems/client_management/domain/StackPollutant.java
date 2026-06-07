package com.ensolution.ems.client_management.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class StackPollutant {
	private Long id;
	private Long stackId;
	private Long pollutantId;

	public static StackPollutant assign(Long stackId, Long pollutantId) {
		return StackPollutant.builder()
			.stackId(stackId)
			.pollutantId(pollutantId)
			.build();
	}
}
