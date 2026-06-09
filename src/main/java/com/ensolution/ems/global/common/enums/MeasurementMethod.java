package com.ensolution.ems.global.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MeasurementMethod {
	DUST("먼지"),
	HEAVY_METAL("중금속"),
	MERCURY("수은"),
	FIELD_MEASUREMENT("현장측정"),
	ABSORPTION_SOLUTION("흡수액"),
	ADSORPTION_TUBE("흡착관"),
	TEDLAR_BAG("테드라백"),
	CARTRIDGE("카트리지");
	
	private final String desc;
}