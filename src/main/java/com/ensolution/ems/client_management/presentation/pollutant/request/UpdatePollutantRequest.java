package com.ensolution.ems.client_management.presentation.pollutant.request;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * 고객사가 관리하는 값만 수정합니다. 전달하지 않은(또는 공백) 필드는 기존 값이 유지됩니다.
 *
 * <p>어떤 가이드 항목인지, 그리고 측정분야·형태는 바꿀 수 없습니다.
 * 다른 물질로 바꾸려면 이 측정물질을 삭제하고 다시 채택합니다. 측정방법은 고객사가 관리하는 값이라 바꿀 수 있습니다.
 *
 * <p>{@code samplingMinutes}(항목별 채취시간)만 예외로 <b>보낸 값이 그대로 저장</b>됩니다 — 비우면 측정방법의
 * 표준 채취시간으로 되돌아갑니다.
 */
public record UpdatePollutantRequest(
	Long methodId,
	@PositiveOrZero
	Integer samplingMinutes,
	String nameKr,
	String nameEn,
	String equipment,
	String testMethod
) {}
