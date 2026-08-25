package com.ensolution.ems.contract.application.service;

import com.ensolution.ems.client_management.application.port.in.ContractSummary;
import com.ensolution.ems.client_management.application.port.in.WorkplaceQueryUseCase;
import com.ensolution.ems.contract.application.command.ContractDetail;
import com.ensolution.ems.contract.domain.Contract;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContractDetailAssembler {

	private final WorkplaceQueryUseCase workplaceQueryUseCase;

	public ContractDetail assemble(Contract contract) {
		ContractSummary summary = workplaceQueryUseCase.getSummaryById(contract.getWorkplaceId(), contract.getTenantId());
		return new ContractDetail(contract, summary.clientName(), summary.workplaceName(), summary.address());
	}
}