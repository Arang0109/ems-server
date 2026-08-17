package com.ensolution.ems.client_management.presentation.pollutant.request;

import jakarta.validation.constraints.NotNull;

/**
 * 지원 물질 가이드에서 측정물질을 채택합니다. 가이드에 없는 물질은 등록할 수 없습니다.
 *
 * <p>측정분야·측정방법·형태는 가이드가 정하므로 요청에 담지 않습니다.
 *
 * @param catalogId 채택할 가이드 항목 id. 선택 목록 응답의 {@code catalogId}를 그대로 보냅니다
 * @param nameKr    비워 두면 가이드의 표준 국문명이 복사됩니다. 이후 값은 고객사가 관리합니다
 */
public record CreatePollutantRequest(
	@NotNull
	Long catalogId,
	String nameKr,
	String nameEn,
	String equipment,
	String testMethod
) {}
