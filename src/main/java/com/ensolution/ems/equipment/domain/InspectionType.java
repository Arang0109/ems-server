package com.ensolution.ems.equipment.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 측정장비가 받아야 하는 검사 종류.
 * <p>
 * 세 종류는 서로 배타적이지 않다. 한 장비가 정도검사와 교정을 동시에 받을 수 있고,
 * 일반시험만 받는 장비도 있다. 그래서 장비는 이 세 종류를 항상 모두 보유하고
 * {@link InspectionItem#enabled()} 로 대상 여부를 표현한다.
 */
@Getter
@AllArgsConstructor
public enum InspectionType {
	PRECISION_INSPECTION("정도검사"),
	CALIBRATION("교정"),
	GENERAL_TEST("일반시험");

	private final String desc;
}
