package com.ensolution.ems.tenant.application.port.in;

public interface WorkplaceQueryUseCase {
	ContractSummary getSummaryById(Long workplaceId, Long tenantId);
	boolean existsById(Long workplaceId);
}