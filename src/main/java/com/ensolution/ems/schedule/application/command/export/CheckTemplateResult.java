package com.ensolution.ems.schedule.application.command.export;

import java.util.List;

/** 템플릿 검사 결과. 문제가 하나도 없으면 유효하다. */
public record CheckTemplateResult(List<TemplateIssue> issues) {

	public boolean valid() {
		return issues == null || issues.isEmpty();
	}
}
