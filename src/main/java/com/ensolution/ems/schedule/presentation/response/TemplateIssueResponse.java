package com.ensolution.ems.schedule.presentation.response;

import com.ensolution.ems.schedule.application.command.export.TemplateExpressionRef;
import com.ensolution.ems.schedule.application.command.export.TemplateIssueType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @param sheetName  시트 이름
 * @param cell       셀 주소(예: {@code B3}). 시트 단위 문제({@code AREA_MISSING})면 null
 * @param source     {@code CELL}(셀 텍스트) 또는 {@code COMMENT}(메모 명령). 시트 단위 문제면 null
 * @param expression 문제가 난 표현식 본문
 * @param name       문제의 이름 — 미해결 경로는 실패 지점까지의 점 경로(예: {@code plan.clientNmae})
 * @param type       문제 종류
 */
@Schema(description = "템플릿 검사에서 발견한 문제 하나")
public record TemplateIssueResponse(
	String sheetName,
	String cell,
	TemplateExpressionRef.Source source,
	String expression,
	String name,
	TemplateIssueType type
) {}
