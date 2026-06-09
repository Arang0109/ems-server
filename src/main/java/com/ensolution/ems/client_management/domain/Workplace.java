package com.ensolution.ems.client_management.domain;

import com.ensolution.ems.global.common.enums.Grade;
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
	private Grade grade;

	public static Workplace register(
		Long companyId,
		String name,
		String address,
		String bizNumber,
		Grade grade
	) {
		return Workplace.builder()
			.companyId(companyId)
			.name(name)
			.address(address)
			.bizNumber(bizNumber)
			.grade(grade)
			.build();
	}

	public Workplace update(Long id, String name, String address, String bizNumber, Grade grade) {
		return this.toBuilder()
			.id(id)
			.name(keep(name, this.name))
			.address(keep(address, this.address))
			.bizNumber(keep(bizNumber, this.bizNumber))
			.grade(grade != null ? grade : this.grade)
			.build();
	}

	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}
}
