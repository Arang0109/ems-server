package com.ensolution.ems.contract.application.command;

import com.ensolution.ems.contract.domain.Contract;

public record ContractDetail(
	Contract contract,
	String companyName,
	String workplaceName,
	String workplaceAddress
) {}
