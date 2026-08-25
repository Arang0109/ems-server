package com.ensolution.ems.client_management.application.port.in;

public interface WorkplaceQueryUseCase {
	ContractSummary getSummaryById(Long workplaceId, Long tenantId);
	/** tenant 범위 존재 확인. 타 tenant의 사업장은 존재하지 않는 것으로 본다. */
	boolean existsById(Long workplaceId, Long tenantId);

	/** 테넌트 소속 사업장 수. */
	long countWorkplaces(Long tenantId);
}