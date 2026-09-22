package com.ensolution.ems.schedule.application.command.export;

/** 템플릿 검사가 구분하는 문제 종류. 렌더링은 이 중 어느 것도 실패로 보지 않고 빈칸으로 넘기므로, 검사가 유일한 방어선이다. */
public enum TemplateIssueType {
	/** 최상위 변수가 아니고 반복 변수도 아닌 이름({@code ${pointz}}). */
	UNKNOWN_ROOT,
	/** 변수는 맞지만 그 아래 프로퍼티가 없음({@code ${plan.clientNmae}}). */
	UNKNOWN_PROPERTY,
	/** {@code custom} 아래 키가 이 고객사의 커스텀 필드 정의에 없음. */
	UNKNOWN_CUSTOM_KEY,
	/** JEXL 문법 오류({@code ${plan..name}}). 렌더링 시 예외로 실패하는 유일한 종류다. */
	PARSE_ERROR,
	/** 셀에 {@code ${...}}가 있는데 그 시트에 {@code jx:area} 메모가 없음 — 영역 밖 표현식은 평가되지 않고 원문이 남는다. */
	AREA_MISSING
}
