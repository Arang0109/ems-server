package com.ensolution.ems.client_management.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Workplace {
	private Long id;
	private Long companyId;
	private String name;
	private String address;
	private String bizNumber;

	public static Workplace register(
		Long companyId,
		String name,
		String address,
		String bizNumber
	) {
		return Workplace.builder()
			.companyId(companyId)
			.name(name)
			.address(address)
			.bizNumber(bizNumber)
			.build();
	}

	public Workplace update(Long id, String name, String address, String bizNumber) {
		return this.toBuilder()
			.id(id)
			.name(keep(name, this.name))
			.address(keep(address, this.address))
			.bizNumber(keep(bizNumber, this.bizNumber))
			.build();
	}

	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}
}
