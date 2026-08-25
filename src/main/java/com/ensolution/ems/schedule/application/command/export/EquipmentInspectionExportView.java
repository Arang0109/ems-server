package com.ensolution.ems.schedule.application.command.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 측정 시점 장비의 검사 항목 하나에 대응하는 엑셀 뷰(jx:each 대상).
 * jxls(JEXL)는 표준 getter로 프로퍼티를 해석하므로 record가 아니라 {@code @Getter} 클래스로 둔다.
 */
@Getter
@Builder
@AllArgsConstructor
public class EquipmentInspectionExportView {

	private final String type;                 // 검사 종류 코드
	private final String typeLabel;            // 검사 종류 한글명
	private final boolean enabled;             // 검사 대상 여부
	private final Integer cycleMonths;         // 검사 주기(개월)
	private final LocalDate lastInspectedAt;   // 최종 수검일
	private final LocalDate nextDueDate;       // 다음 검사 예정일
}
