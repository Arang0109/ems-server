package com.ensolution.ems.tenant.domain;

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
	private Long tenantId;
	private Long clientId;
	private String name;
	private String bizNumber;
	private String roadAddress;
	private String detailAddress;
	private String zipcode;
	private Grade grade;

	public static Workplace register(
		Long tenantId,
		Long clientId,
		String name,
		String bizNumber,
		String roadAddress,
		String detailAddress,
		String zipcode,
		Grade grade
	) {
		return Workplace.builder()
			.tenantId(tenantId)
			.clientId(clientId)
			.name(name)
			.bizNumber(bizNumber)
			.roadAddress(roadAddress)
			.detailAddress(detailAddress)
			.zipcode(zipcode)
			.grade(grade)
			.build();
	}

	public Workplace update(
		String name,
		String bizNumber,
		String roadAddress,
		String detailAddress,
		String zipcode,
		Grade grade) {
		return this.toBuilder()
			.name(keep(name, this.name))
			.bizNumber(keep(bizNumber, this.bizNumber))
			.roadAddress(keep(roadAddress, this.roadAddress))
			.detailAddress(keep(detailAddress, this.detailAddress))
			.zipcode(keep(zipcode, this.zipcode))
			.grade(grade != null ? grade : this.grade)
			.build();
	}

	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}
}
