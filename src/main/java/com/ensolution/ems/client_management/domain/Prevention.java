package com.ensolution.ems.client_management.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Prevention {
	private Long id;
	private Long tenantId;
	private Long stackId;
	private String name;
	private Double capacity;
	private String targetName;
	private String removalEfficiency;

	public static Prevention register(Long tenantId, Long stackId, String name, Double capacity, String targetName, String removalEfficiency) {
		return Prevention.builder().tenantId(tenantId).stackId(stackId).name(name).capacity(capacity).targetName(targetName).removalEfficiency(removalEfficiency).build();
	}

	public Prevention update(String name, Double capacity, String targetName, String removalEfficiency) {
		return this.toBuilder()
			.name(keep(name, this.name))
			.capacity(capacity != null ? capacity : this.capacity)
			.targetName(keep(targetName, this.targetName))
			.removalEfficiency(keep(removalEfficiency, this.removalEfficiency))
			.build();
	}

	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}
}
