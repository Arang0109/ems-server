package com.ensolution.ems.equipment.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 검사 판정. 판정 개념이 없는 검사(성적서만 발급)는 null을 허용한다. */
@Getter
@AllArgsConstructor
public enum InspectionResult {
	PASS("적합"),
	FAIL("부적합");

	private final String desc;
}
