package com.ensolution.ems.client_management.presentation.pollutant.request;

/**
 * 고객사가 관리하는 값만 수정합니다. 전달하지 않은(또는 공백) 필드는 기존 값이 유지됩니다.
 *
 * <p>어떤 가이드 항목인지, 그리고 측정분야·측정방법·형태는 바꿀 수 없습니다.
 * 다른 물질로 바꾸려면 이 측정물질을 삭제하고 다시 채택합니다.
 */
public record UpdatePollutantRequest(
	String nameKr,
	String nameEn,
	String equipment,
	String testMethod
) {}
