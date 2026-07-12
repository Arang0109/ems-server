package com.ensolution.ems.contract.application.command;

import com.ensolution.ems.contract.domain.Contract;

public record ContractDetail(
	Contract contract,
	String clientName,
	String workplaceName,
	String workplaceAddress
) {}
