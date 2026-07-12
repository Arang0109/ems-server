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
	private Long clientId;
	private String name;
	private String zipcode;
	private String roadAddress;
	private String address;
	private String bizNumber;
	private Grade grade;

	public static Workplace register(
		Long clientId,
		String name,
		String zipcode,
		String roadAddress,
		String address,
		String bizNumber,
		Grade grade
	) {
		return Workplace.builder()
			.clientId(clientId)
			.name(name)
			.zipcode(zipcode)
			.roadAddress(roadAddress)
			.address(address)
			.bizNumber(bizNumber)
			.grade(grade)
			.build();
	}

	public Workplace update(
		String name,
		String zipcode,
		String roadAddress,
		String address,
		String bizNumber,
		Grade grade) {
		return this.toBuilder()
			.name(keep(name, this.name))
			.zipcode(keep(zipcode, this.zipcode))
			.roadAddress(keep(roadAddress, this.roadAddress))
			.address(keep(address, this.address))
			.bizNumber(keep(bizNumber, this.bizNumber))
			.grade(grade != null ? grade : this.grade)
			.build();
	}

	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}
}
