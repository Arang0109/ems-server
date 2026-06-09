package com.ensolution.ems.client_management.domain;

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
	private Long preventionId;
	private String name;
	private Double removalEfficiency;
	
	public static TargetSubstance register(Long preventionId, String name, Double removalEfficiency) {
		return TargetSubstance.builder()
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
