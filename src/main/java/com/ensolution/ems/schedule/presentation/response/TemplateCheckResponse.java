package com.ensolution.ems.schedule.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "채취기록부 템플릿 검사 결과. issues 가 비어 있으면 valid 입니다.")
public record TemplateCheckResponse(
	boolean valid,
	List<TemplateIssueResponse> issues
) {}
