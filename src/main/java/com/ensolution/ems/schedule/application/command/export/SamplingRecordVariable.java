package com.ensolution.ems.schedule.application.command.export;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * 채취기록부 템플릿의 <b>최상위 변수</b>. 변수 이름이 곧 고객 템플릿의 계약이다.
 *
 * <p>렌더러({@code JxlsSheetExcelRenderer})는 여기서 <b>값</b>을 뽑아 jxls 컨텍스트에 담고, 템플릿 검사기
 * ({@code UnknownExpressionFinder})는 여기서 <b>타입</b>을 읽어 표현식 경로를 대조한다. 목록이 둘이면 반드시
 * 어긋나므로 한 곳에 둔다. {@code ~ExportView}와 같은 "jxls 바인딩 계약"이라 이 패키지에 있다.
 *
 * <p>시트의 측정 영역별 하위 뷰({@code weather}·{@code moisture}…)를 최상위로도 노출하는 것은 템플릿이
 * {@code ${sheet.moisture.xw}} 대신 {@code ${moisture.xw}}로 짧게 쓰게 하기 위해서다. 측정항목({@code items})은
 * 시트가 아니라 계획에 딸린 값이지만 기록부 서식도 측정항목 칸을 가지므로 함께 노출한다.
 *
 * <p>{@code custom}은 테넌트가 정의한 커스텀 필드의 회차 값(Map)이다. JEXL은 Map도 {@code custom.key}로 해석하므로
 * 타입 계약({@code ~ExportView})을 건드리지 않고 이름 공간 하나로 자유도를 연다.
 */
@Getter
public enum SamplingRecordVariable {

	PLAN("plan", ScheduleExportView.class, null, (plan, sheet) -> plan),
	SHEET("sheet", SheetExportView.class, null, (plan, sheet) -> sheet),
	WEATHER("weather", WeatherExportView.class, null, (plan, sheet) -> sheet.getWeather()),
	MOISTURE("moisture", MoistureExportView.class, null, (plan, sheet) -> sheet.getMoisture()),
	GAS("gas", GasExportView.class, null, (plan, sheet) -> sheet.getGas()),
	FLOW("flow", FlowExportView.class, null, (plan, sheet) -> sheet.getFlow()),
	PARTICLE("particle", ParticleExportView.class, null, (plan, sheet) -> sheet.getParticle()),
	POINTS("points", List.class, PointExportView.class, (plan, sheet) -> sheet.getPoints()),
	GASEOUS_SAMPLINGS("gaseousSamplings", List.class, SampleExportView.class, (plan, sheet) -> sheet.getGaseousSamplings()),
	ITEMS("items", List.class, SamplingItemExportView.class, (plan, sheet) -> plan.getItems()),
	CUSTOM("custom", Map.class, null, (plan, sheet) -> plan.getCustomFields());

	/** 템플릿이 쓰는 이름. */
	private final String variableName;
	/** 변수의 타입. 목록이면 {@code List.class}, 커스텀 필드면 {@code Map.class}. */
	private final Class<?> type;
	/** 목록일 때 원소 타입. 아니면 null. */
	private final Class<?> elementType;
	private final BiFunction<ScheduleExportView, SheetExportView, Object> extractor;

	SamplingRecordVariable(String variableName, Class<?> type, Class<?> elementType,
	                       BiFunction<ScheduleExportView, SheetExportView, Object> extractor) {
		this.variableName = variableName;
		this.type = type;
		this.elementType = elementType;
		this.extractor = extractor;
	}

	/** 한 장의 기록부에 바인딩할 값. 하위 뷰는 매퍼가 항상 채우므로 null 검사는 필요하지 않다. */
	public Object extract(ScheduleExportView plan, SheetExportView sheet) {
		return extractor.apply(plan, sheet);
	}

	public boolean isList() {
		return type == List.class;
	}

	public static Optional<SamplingRecordVariable> byName(String variableName) {
		return Arrays.stream(values())
			.filter(v -> v.variableName.equals(variableName))
			.findFirst();
	}
}
