package com.ensolution.ems.platform.application.result;

/**
 * 타 모듈(schedule 등)이 측정 시점 고객사 스냅샷을 구성할 때 사용하는 테넌트 요약 VO.
 * 상호·사업자번호·대표자와 주소를 값 그대로 노출하며, 구독 플랜·상태 등 운영 정보는 포함하지 않는다.
 */
public record TenantSummary(
	Long tenantId,
	String name,
	String bizNumber,
	String representative,
	String roadAddress,
	String detailAddress,
	String zipcode
) {}
