package com.ensolution.ems.schedule.application.command.export;

/**
 * 템플릿 검사에서 발견한 문제 하나.
 *
 * @param sheetName   시트 이름
 * @param cellAddress 셀 주소. 시트 단위 문제({@link TemplateIssueType#AREA_MISSING})면 null
 * @param source      셀 텍스트인지 메모 명령인지. 시트 단위 문제면 null
 * @param expression  문제가 난 표현식 본문
 * @param name        문제의 이름 — 미해결 경로는 실패 지점까지의 점 경로({@code plan.clientNmae}), 파싱 오류는 표현식 자체
 * @param type        문제 종류
 */
public record TemplateIssue(
	String sheetName,
	String cellAddress,
	TemplateExpressionRef.Source source,
	String expression,
	String name,
	TemplateIssueType type
) {}
