package com.ensolution.ems.schedule.application.command.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 측정 시점 배출시설 하나에 대응하는 엑셀 뷰.
 * 원장 연결키(facilityId)는 템플릿에서 쓰이지 않으므로 노출하지 않는다.
 * jxls(JEXL)는 표준 getter로 프로퍼티를 해석하므로 record가 아니라 {@code @Getter} 클래스로 둔다.
 */
@Getter
@Builder
@AllArgsConstructor
public class FacilityExportView {

	private final String name;               // 시설명
	private final String fuelUsage;          // 연료 사용량
	private final String productOutput;      // 제품 생산량
	private final String incinerationAmount; // 소각량
	private final String fuelInput;          // 원료 투입량
	private final String fuelType;           // 연료·원료 종류
	private final String unit;               // 사용량·생산량에 적용되는 단위
}
