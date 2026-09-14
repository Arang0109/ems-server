package com.ensolution.ems.client_management.application.command.create;

import java.math.BigDecimal;

/**
 * 가이드 항목 채택. 측정분야·형태는 카탈로그가 소유하므로 여기서 받지 않는다.
 * 측정방법은 같은 물질이라도 업체마다 다를 수 있어 고객사가 채택 시 정한다.
 *
 * @param catalogId 채택할 가이드 항목 id. 고객사는 가이드에 없는 물질을 만들 수 없으므로 필수다
 * @param methodId  이 고객사가 이 물질에 쓰는 측정방법 id. 필수 — Request 검증이 보장하고, 소유는 Validator가 확인한다
 * @param samplingMinutes 항목별 채취시간 오버라이드(분). null이면 측정방법 기본값. MERGED 방법에는 둘 수 없다
 * @param suctionFlowRate 항목별 흡인유량 오버라이드(L/min). 규칙은 samplingMinutes와 같다
 * @param nameKr    비워 두면 카탈로그의 표준 국문명을 복사한다
 */
public record CreatePollutantCommand(
	Long tenantId,
	Long catalogId,
	Long methodId,
	Integer samplingMinutes,
	BigDecimal suctionFlowRate,
	String nameKr,
	String nameEn,
	String equipment,
	String testMethod
) {}
