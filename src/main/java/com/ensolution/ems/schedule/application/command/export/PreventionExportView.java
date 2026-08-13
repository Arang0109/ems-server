package com.ensolution.ems.schedule.application.command.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 측정 시점 방지시설 하나에 대응하는 엑셀 뷰. 대상물질명·제거효율을 자체 필드로 함께 보관한다.
 * 원장 연결키(preventionId)는 템플릿에서 쓰이지 않으므로 노출하지 않는다.
 * jxls(JEXL)는 표준 getter로 프로퍼티를 해석하므로 record가 아니라 {@code @Getter} 클래스로 둔다.
 */
@Getter
@Builder
@AllArgsConstructor
public class PreventionExportView {

	private final String name;                // 시설명
	private final Double capacity;            // 용량
	private final String unit;
	private final String targetName;          // 대상물질명
	private final String removalEfficiency;   // 제거효율
}
