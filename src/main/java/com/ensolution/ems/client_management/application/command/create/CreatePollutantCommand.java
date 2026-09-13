package com.ensolution.ems.client_management.application.command.create;

import com.ensolution.ems.global.common.enums.MeasurementMethod;

/**
 * 가이드 항목 채택. 측정분야·형태는 카탈로그가 소유하므로 여기서 받지 않는다.
 * 측정방법은 같은 물질이라도 업체마다 다를 수 있어 고객사가 채택 시 정한다.
 *
 * @param catalogId 채택할 가이드 항목 id. 고객사는 가이드에 없는 물질을 만들 수 없으므로 필수다
 * @param method    이 고객사가 이 물질에 쓰는 측정방법. 필수 — Request 검증이 보장한다
 * @param nameKr    비워 두면 카탈로그의 표준 국문명을 복사한다
 */
public record CreatePollutantCommand(
	Long tenantId,
	Long catalogId,
	MeasurementMethod method,
	String nameKr,
	String nameEn,
	String equipment,
	String testMethod
) {}
