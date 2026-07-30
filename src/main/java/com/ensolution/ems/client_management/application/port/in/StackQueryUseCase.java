package com.ensolution.ems.client_management.application.port.in;

/**
 * 측정 대상(측정시설과 상·하위 트리, 시설별 측정항목) 정보를 타 모듈에 제공하는 인바운드 포트.
 * 소유권 격리를 위해 tenantId를 함께 받으며, 미존재·타 tenant는 NOT_FOUND로 은닉한다.
 */
public interface StackQueryUseCase {
	StackMeasurementSummary getMeasurementTargetSummary(Long stackId, Long tenantId);

	/** 테넌트 소속 측정시설(굴뚝) 수. */
	long countStacks(Long tenantId);
}
