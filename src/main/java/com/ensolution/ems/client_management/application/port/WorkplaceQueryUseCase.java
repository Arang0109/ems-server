package com.ensolution.ems.client_management.application.port;

public interface WorkplaceQueryUseCase {
	ContractSummary getSummaryById(Long workplaceId);
	boolean existsById(Long workplaceId);
}