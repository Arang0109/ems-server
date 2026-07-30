package com.ensolution.ems.client_management.domain;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;
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
	private Long tenantId;
	private MeasurementField field;
	private String nameKr;
	private String nameEn;
	private MeasurementMethod method;
	private PollutantPhase phase;
	private String equipment;
	private String testMethod;
	
	public static Pollutant register(
		Long tenantId,
		MeasurementField field,
		String nameKr,
		String nameEn,
		MeasurementMethod method,
		PollutantPhase phase,
		String equipment,
		String testMethod
	) {
		return Pollutant.builder()
			.tenantId(tenantId)
			.field(field)
			.nameKr(nameKr)
			.nameEn(nameEn)
			.method(method)
			.phase(phase)
			.equipment(equipment)
			.testMethod(testMethod)
			.build();
	}
	
	public Pollutant update(
		Long id,
		MeasurementField field,
		String nameKr,
		String nameEn,
		MeasurementMethod method,
		PollutantPhase phase,
		String equipment,
		String testMethod
	) {
		return this.toBuilder()
			.id(id)
			.field(keep(field, this.field))
			.nameKr(keep(nameKr, this.nameKr))
			.nameEn(keep(nameEn, this.nameEn))
			.method(keep(method, this.method))
			.phase(keep(phase, this.phase))
			.equipment(keep(equipment, this.equipment))
			.testMethod(keep(testMethod, this.testMethod))
			.build();
	}
	
	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}
	
	private static <T> T keep(T value, T original) {
		return value == null ? original : value;
	}
}
