package com.ensolution.ems.tenant.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class TargetSubstance {
	private Long id;
	private Long tenantId;
	private Long preventionId;
	private String name;
	private Double removalEfficiency;

	public static TargetSubstance register(Long tenantId, Long preventionId, String name, Double removalEfficiency) {
		return TargetSubstance.builder()
			.tenantId(tenantId)
			.preventionId(preventionId)
			.name(name)
			.removalEfficiency(removalEfficiency)
			.build();
	}
	
	public TargetSubstance update(String name, Double removalEfficiency) {
		return this.toBuilder()
			.name(keep(name, this.name))
			.removalEfficiency(removalEfficiency)
			.build();
	}
	
	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}
}
