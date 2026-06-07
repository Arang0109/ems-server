package com.ensolution.ems.client_management.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Pollutant {
	private Long id;
	private String nameKr;
	private String nameEn;

	public static Pollutant register(String nameKr, String nameEn) {
		return Pollutant.builder()
			.nameKr(nameKr)
			.nameEn(nameEn)
			.build();
	}

	public Pollutant update(String nameKr, String nameEn) {
		return this.toBuilder()
			.nameKr(keep(nameKr, this.nameKr))
			.nameEn(keep(nameEn, this.nameEn))
			.build();
	}

	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}
}
