package com.ensolution.ems.contract.application;

import com.ensolution.ems.client_management.domain.port.WorkplaceRepository;
import com.ensolution.ems.contract.application.command.ContractListItem;
import com.ensolution.ems.contract.application.command.CreateContractCommand;
import com.ensolution.ems.contract.application.command.UpdateContractCommand;
import com.ensolution.ems.contract.domain.Contract;
import com.ensolution.ems.contract.domain.port.ContractRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractService {

	private final ContractRepository contractRepository;
	private final WorkplaceRepository workplaceRepository;

	public Contract createContract(CreateContractCommand command) {
		workplaceRepository.findById(command.workplaceId());
		Contract contract = Contract.register(
			command.workplaceId(),
			command.contractName(),
			command.contractDate(),
			command.startDate(),
			command.completionDate(),
			command.contractAccount(),
			command.contractAmountUnit(),
			command.vatIncluded(),
			command.contractGuaranteeAmount(),
			command.advancePaymentAmount(),
			command.advancePaymentDueDate(),
			command.delayPenaltyRate(),
			command.remark()
		);
		return contractRepository.save(contract);
	}

	public Contract getContract(Long contractId) {
		return contractRepository.findById(contractId);
	}

	public List<ContractListItem> getContractList(Long workplaceId) {
		return contractRepository.findByWorkplaceId(workplaceId);
	}

	public Contract updateContract(Long contractId, UpdateContractCommand command) {
		Contract contract = contractRepository.findById(contractId);
		Contract updated = contract.update(
			command.contractName(),
			command.contractDate(),
			command.startDate(),
			command.completionDate(),
			command.contractAccount(),
			command.contractAmountUnit(),
			command.vatIncluded(),
			command.contractGuaranteeAmount(),
			command.advancePaymentAmount(),
			command.advancePaymentDueDate(),
			command.delayPenaltyRate(),
			command.remark()
		);
		return contractRepository.save(updated);
	}

	public void deleteContract(Long contractId) {
		contractRepository.deleteById(contractId);
	}
}
