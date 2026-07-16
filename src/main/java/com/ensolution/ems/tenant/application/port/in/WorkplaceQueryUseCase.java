package com.ensolution.ems.tenant.application.port.in;

public interface WorkplaceQueryUseCase {
	ContractSummary getSummaryById(Long workplaceId, Long tenantId);
	boolean existsById(Long workplaceId);

	/** 테넌트 소속 사업장 수. */
	long countWorkplaces(Long tenantId);
}