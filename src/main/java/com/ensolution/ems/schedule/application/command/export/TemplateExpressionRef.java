package com.ensolution.ems.schedule.application.command.export;

import java.util.List;
import java.util.Map;

/**
 * 업로드된 템플릿에서 읽어 낸 jxls 표현식 하나. 셀 텍스트의 {@code ${...}} 또는 셀 메모의 {@code jx:} 명령이다.
 * 템플릿 리더({@code ExcelTemplateReader})가 만들고 검사기({@code UnknownExpressionFinder})가 계약과 대조한다.
 *
 * @param sheetName     시트 이름
 * @param cellAddress   셀 주소({@code "B3"})
 * @param source        셀 텍스트인지 메모 명령인지
 * @param command       메모 명령 이름({@code each}·{@code if}·{@code area}…). 셀 텍스트면 null
 * @param attributes    메모 명령의 속성({@code items}·{@code var}·{@code lastCell}…). 셀 텍스트면 빈 맵
 * @param expression    평가되는 표현식 본문 — 셀 텍스트면 {@code ${}} 안의 문자열, {@code each}면 {@code items} 값,
 *                      {@code if}면 {@code condition} 값. 평가할 표현식이 없는 명령({@code area})은 null
 * @param variablePaths 표현식에서 참조하는 변수 경로들. {@code items[0].name} → {@code [items, 0, name]},
 *                      {@code custom['x']} → {@code [custom, x]}. 파싱 실패면 빈 목록
 * @param parsable      표현식이 JEXL 문법에 맞는지. {@code expression}이 null이면 true
 */
public record TemplateExpressionRef(
	String sheetName,
	String cellAddress,
	Source source,
	String command,
	Map<String, String> attributes,
	String expression,
	List<List<String>> variablePaths,
	boolean parsable
) {
	public enum Source { CELL, COMMENT }

	public boolean isCommand(String name) {
		return source == Source.COMMENT && name.equals(command);
	}

	public String attribute(String name) {
		return attributes == null ? null : attributes.get(name);
	}
}
