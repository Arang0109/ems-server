package com.ensolution.ems.global.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MeasurementField {
	AIR("대기"),
	WATER("수질"),
	NOISE_VIBRATION("소음진동"),
	ODOR("악취");
	
	private final String desc;
}