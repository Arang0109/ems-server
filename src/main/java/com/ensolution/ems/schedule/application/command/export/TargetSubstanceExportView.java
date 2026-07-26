package com.ensolution.ems.schedule.application.command.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 측정 시점 측정대상물질 하나에 대응하는 엑셀 뷰.
 * 방지시설 하위({@link PreventionExportView#getTargetSubstances()})와 최상위 평탄 목록
 * ({@link ScheduleExportView#getTargetSubstances()}) 양쪽에서 같은 인스턴스를 공유하므로,
 * 평탄 목록에서 소속을 구분할 수 있도록 {@code preventionName}을 함께 보관한다.
 * jxls(JEXL)는 표준 getter로 프로퍼티를 해석하므로 record가 아니라 {@code @Getter} 클래스로 둔다.
 */
@Getter
@Builder
@AllArgsConstructor
public class TargetSubstanceExportView {

	private final String preventionName;     // 소속 방지시설명
	private final String name;               // 물질명
	private final Double removalEfficiency;  // 제거효율(%)
}
