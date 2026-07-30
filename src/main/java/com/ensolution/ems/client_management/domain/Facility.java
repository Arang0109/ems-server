package com.ensolution.ems.client_management.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Facility {
	private Long id;
	private Long tenantId;
	private Long stackId;
	private String name;
	private String fuelUsage;          // 연료 사용량
	private String productOutput;      // 제품 생산량
	private String incinerationAmount; // 소각량
	private String fuelInput;          // 원료 투입량
	private String fuelType;           // 연료·원료 종류
	private String unit;               // 사용량·생산량에 적용되는 단위

	public static Facility register(
		Long tenantId, Long stackId, String name, String fuelUsage, String productOutput,
		String incinerationAmount, String fuelInput, String fuelType, String unit) {
		return Facility.builder()
			.tenantId(tenantId)
			.stackId(stackId)
			.name(name)
			.fuelUsage(fuelUsage)
			.productOutput(productOutput)
			.incinerationAmount(incinerationAmount)
			.fuelInput(fuelInput)
			.fuelType(fuelType)
			.unit(unit)
			.build();
	}

	public Facility update(
		String name, String fuelUsage, String productOutput,
		String incinerationAmount, String fuelInput, String fuelType, String unit) {
		return this.toBuilder()
			.name(keep(name, this.name))
			.fuelUsage(keep(fuelUsage, this.fuelUsage))
			.productOutput(keep(productOutput, this.productOutput))
			.incinerationAmount(keep(incinerationAmount, this.incinerationAmount))
			.fuelInput(keep(fuelInput, this.fuelInput))
			.fuelType(keep(fuelType, this.fuelType))
			.unit(keep(unit, this.unit))
			.build();
	}

	private static String keep(String value, String original) {
			return value == null || value.isBlank() ? original : value;
	}
}
