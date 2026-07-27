package com.ensolution.ems.schedule.application.command.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 측정 시트 하나에 대응하는 엑셀 뷰. 입력값과 계산값을 모두 노출한다(고객 양식에 자체 계산식이 있을 수 있으므로 원시 입력도 제공).
 * 값은 측정 영역별 하위 뷰로 묶어 {@code sheet.moisture.ratio}처럼 짧은 경로로 참조하게 한다.
 * <p>
 * <b>하위 뷰 5개(weather·moisture·gas·flow·particle)는 항상 non-null이다.</b> 해당 영역의 데이터가 없으면
 * 전 필드가 null인 빈 뷰가 들어간다. 렌더러가 {@code withExceptionThrower()}로 동작하므로 하위 뷰가 null이면
 * 템플릿이 곧바로 실패하기 때문이며, 이 계약은 {@code SheetExportViewMapper}가 보장한다.
 * 유량(flow)은 항상, 입자상(particle) 집계는 입자상 시트일 때만 값이 채워진다.
 * <p>
 * 측정점별 값은 {@link PointExportView}, 시료 채취 정보는 {@link SampleExportView} 목록으로 노출한다.
 * jxls(JEXL)는 표준 getter로 프로퍼티를 해석하므로 record가 아니라 {@code @Getter} 클래스로 둔다.
 */
@Getter
@Builder
@AllArgsConstructor
public class SheetExportView {

	private final String category;      // 측정 카테고리 (가스상/중금속/먼지/수은)
	private final Integer pointCount;   // 규정상 요구 측정점 수

	// ===== 측정 영역별 하위 뷰 (항상 non-null) =====
	private final WeatherExportView weather;
	private final MoistureExportView moisture;
	private final GasExportView gas;
	private final FlowExportView flow;
	private final ParticleExportView particle;

	// ===== 목록 (jx:each 대상) =====
	private final List<PointExportView> points;
	private final List<SampleExportView> samples;
}
