package com.ensolution.ems.client_management.application.port.in;

public interface WorkplaceQueryUseCase {
	ContractSummary getSummaryById(Long workplaceId);
	boolean existsById(Long workplaceId);
}