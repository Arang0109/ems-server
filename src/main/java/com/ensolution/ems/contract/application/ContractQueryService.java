package com.ensolution.ems.contract.application;

import com.ensolution.ems.contract.application.command.ContractDetail;
import com.ensolution.ems.contract.application.command.ContractListItem;
import com.ensolution.ems.contract.domain.port.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractQueryService {

	private final ContractRepository contractRepository;
	private final ContractDetailAssembler assembler;

	public ContractDetail getContract(Long contractId) {
		return assembler.assemble(contractRepository.findById(contractId));
	}

	public List<ContractListItem> getContractList(Long workplaceId) {
		return workplaceId == null
			? contractRepository.findAll()
			: contractRepository.findByWorkplaceId(workplaceId);
	}
}
