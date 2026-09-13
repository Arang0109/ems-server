package com.ensolution.ems.client_management.presentation.pollutant.request;

import com.ensolution.ems.global.common.enums.MeasurementMethod;
import jakarta.validation.constraints.NotNull;

/**
 * 지원 물질 가이드에서 측정물질을 채택합니다. 가이드에 없는 물질은 등록할 수 없습니다.
 *
 * <p>측정분야·형태는 가이드가 정하므로 요청에 담지 않습니다. 측정방법은 같은 물질이라도
 * 업체마다 다를 수 있어(예: 이황화메틸은 테드라백·카트리지 둘 다 허용) 고객사가 채택 시 <b>필수로</b> 지정합니다.
 *
 * @param catalogId 채택할 가이드 항목 id. 선택 목록 응답의 {@code catalogId}를 그대로 보냅니다
 * @param method    이 고객사가 이 물질에 쓰는 측정방법
 * @param nameKr    비워 두면 가이드의 표준 국문명이 복사됩니다. 이후 값은 고객사가 관리합니다
 */
public record CreatePollutantRequest(
	@NotNull
	Long catalogId,
	@NotNull
	MeasurementMethod method,
	String nameKr,
	String nameEn,
	String equipment,
	String testMethod
) {}
