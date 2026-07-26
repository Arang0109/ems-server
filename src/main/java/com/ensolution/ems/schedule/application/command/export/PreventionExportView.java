package com.ensolution.ems.schedule.application.command.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 측정 시점 방지시설 하나에 대응하는 엑셀 뷰. 하위로 측정대상물질 뷰를 품는다.
 * 원장 연결키(preventionId)는 템플릿에서 쓰이지 않으므로 노출하지 않으며,
 * 목록 필드는 null이 아닌 빈 리스트다(템플릿의 jx:each가 null에서 깨지지 않도록).
 * jxls(JEXL)는 표준 getter로 프로퍼티를 해석하므로 record가 아니라 {@code @Getter} 클래스로 둔다.
 */
@Getter
@Builder
@AllArgsConstructor
public class PreventionExportView {

	private final String name;      // 시설명
	private final Double capacity;  // 용량

	// 측정대상물질 목록 (jx:each 대상)
	private final List<TargetSubstanceExportView> targetSubstances;
}
