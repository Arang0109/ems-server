package com.ensolution.ems.tenant.domain;

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

	public static Prevention register(Long tenantId, Long stackId, String name) {
		return Prevention.builder().tenantId(tenantId).stackId(stackId).name(name).build();
	}

	public Prevention update(String name) {
		return this.toBuilder()
			.name(keep(name, this.name))
			.build();
	}

	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}
}
