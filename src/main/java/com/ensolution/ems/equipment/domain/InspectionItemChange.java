package com.ensolution.ems.equipment.domain;

import java.time.LocalDate;

/**
 * 장비 수정 시 특정 검사 종류에 적용할 변경분.
 * <p>
 * 플래그를 {@code Boolean} 으로 받는 이유는 "false로 끄기"와 "미전달"을 구분해야 하기 때문이다.
 * {@code boolean} 으로 받으면 전달하지 않은 검사가 매번 false로 내려가 조용히 꺼진다.
 * 요청에 없는 검사 종류는 아예 손대지 않는다.
 */
public record InspectionItemChange(
	InspectionType type,
	Boolean enabled,
	Integer cycleMonths,
	LocalDate lastInspectedAt,
	LocalDate nextDueDateOverride,
	Boolean notificationEnabled
) {
}
