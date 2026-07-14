package com.ensolution.ems.tenant.domain;

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
	private String fuelUsage;
	private String fuelInput;
	private String fuelType;

	public static Facility register(Long tenantId, Long stackId, String name, String fuelUsage, String fuelInput, String fuelType) {
		return Facility.builder()
			.tenantId(tenantId)
			.stackId(stackId)
			.name(name)
			.fuelUsage(fuelUsage)
			.fuelInput(fuelInput)
			.fuelType(fuelType)
			.build();
	}
	
	public Facility update(String name, String fuelUsage, String fuelInput, String fuelType) {
		return this.toBuilder()
			.name(keep(name, this.name))
			.fuelUsage(keep(fuelUsage, this.fuelUsage))
			.fuelInput(keep(fuelInput, this.fuelInput))
			.fuelType(keep(fuelType, this.fuelType))
			.build();
	}

	private static String keep(String value, String original) {
			return value == null || value.isBlank() ? original : value;
	}
}
