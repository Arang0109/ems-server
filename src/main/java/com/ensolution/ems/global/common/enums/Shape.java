package com.ensolution.ems.global.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Shape {
	CIRCULAR("원형"),
	RECTANGULAR("사각형");
	
	private final String desc;
}
