package com.ensolution.ems.contract.application;

import com.ensolution.ems.client_management.application.port.in.WorkplaceQueryUseCase;
import com.ensolution.ems.contract.application.command.ContractDetail;
import com.ensolution.ems.contract.application.command.ContractListItem;
import com.ensolution.ems.contract.application.command.CreateContractCommand;
import com.ensolution.ems.contract.application.command.UpdateContractCommand;
import com.ensolution.ems.contract.application.port.ContractQueryUseCase;
import com.ensolution.ems.contract.domain.Contract;
import com.ensolution.ems.contract.domain.port.ContractRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractService implements ContractQueryUseCase {

	private final ContractRepository contractRepository;
	private final WorkplaceQueryUseCase workplaceQueryUseCase;
	private final ContractDetailAssembler assembler;

	public ContractDetail createContract(CreateContractCommand command) {
		if (!workplaceQueryUseCase.existsById(command.workplaceId())) {
			throw new CustomException(ErrorCode.NOT_FOUND);
		}

		Contract saved = contractRepository.save(Contract.register(
			command.workplaceId(),
			command.contractName(),
			command.contractDate(),
			command.startDate(),
			command.completionDate(),
			command.contractAmount(),
			command.contractAmountUnit(),
			command.vatIncluded(),
			command.contractGuaranteeAmount(),
			command.advancePaymentAmount(),
			command.advancePaymentDueDate(),
			command.delayPenaltyRate(),
			command.remark()
		));
		return assembler.assemble(saved);
	}

	public ContractDetail updateContract(Long contractId, UpdateContractCommand command) {
		Contract contract = contractRepository.findById(contractId);
		Contract saved = contractRepository.save(contract.update(
			contractId,
			command.contractName(),
			command.contractDate(),
			command.startDate(),
			command.completionDate(),
			command.contractAmount(),
			command.contractAmountUnit(),
			command.vatIncluded(),
			command.contractGuaranteeAmount(),
			command.advancePaymentAmount(),
			command.advancePaymentDueDate(),
			command.delayPenaltyRate(),
			command.remark()
		));
		return assembler.assemble(saved);
	}

	public void deleteContract(Long contractId) {
		contractRepository.deleteById(contractId);
	}

	@Override
	public void deleteContracts(Long workplaceId) {
		contractRepository.deleteByWorkplaceId(workplaceId);
	}

	@Transactional(readOnly = true)
	public ContractDetail getContract(Long contractId) {
		return assembler.assemble(contractRepository.findById(contractId));
	}

	@Transactional(readOnly = true)
	public List<ContractListItem> getContractList(Long workplaceId) {
		return workplaceId == null
			? contractRepository.findAll()
			: contractRepository.findByWorkplaceId(workplaceId);
	}
}
