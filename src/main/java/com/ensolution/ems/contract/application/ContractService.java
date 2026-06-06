package com.ensolution.ems.contract.application;

import com.ensolution.ems.client_management.application.port.ContractSummary;
import com.ensolution.ems.client_management.application.port.WorkplaceQueryUseCase;
import com.ensolution.ems.contract.application.command.ContractDetail;
import com.ensolution.ems.contract.application.command.ContractListItem;
import com.ensolution.ems.contract.application.command.CreateContractCommand;
import com.ensolution.ems.contract.application.command.UpdateContractCommand;
import com.ensolution.ems.contract.domain.Contract;
import com.ensolution.ems.contract.domain.port.ContractRepository;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractService {

	private final ContractRepository contractRepository;
	private final WorkplaceQueryUseCase workplaceQueryUseCase;

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
			command.contractAccount(),
			command.contractAmountUnit(),
			command.vatIncluded(),
			command.contractGuaranteeAmount(),
			command.advancePaymentAmount(),
			command.advancePaymentDueDate(),
			command.delayPenaltyRate(),
			command.remark()
		));
		ContractSummary summary = workplaceQueryUseCase.getSummaryById(saved.getWorkplaceId());
		return new ContractDetail(saved, summary.companyName(), summary.workplaceName());
	}

	public ContractDetail getContract(Long contractId) {
		Contract contract = contractRepository.findById(contractId);
		ContractSummary summary = workplaceQueryUseCase.getSummaryById(contract.getWorkplaceId());
		return new ContractDetail(contract, summary.companyName(), summary.workplaceName());
	}

	public List<ContractListItem> getContractList(Long workplaceId) {
		List<ContractListItem> contracts = workplaceId == null
			? contractRepository.findAll()
			: contractRepository.findByWorkplaceId(workplaceId);

		List<Long> workplaceIds = contracts.stream()
			.map(ContractListItem::workplaceId)
			.distinct()
			.toList();

		Map<Long, List<MeasurementField>> fieldsMap =
			workplaceQueryUseCase.getFieldsByWorkplaceIds(workplaceIds);

		return contracts.stream()
			.map(c -> c.withFieldList(fieldsMap.getOrDefault(c.workplaceId(), List.of())))
			.toList();
	}

	public ContractDetail updateContract(Long contractId, UpdateContractCommand command) {
		Contract contract = contractRepository.findById(contractId);
		Contract saved = contractRepository.save(contract.update(
			contractId,
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
		));
		ContractSummary summary = workplaceQueryUseCase.getSummaryById(saved.getWorkplaceId());
		return new ContractDetail(saved, summary.companyName(), summary.workplaceName());
	}

	public void deleteContract(Long contractId) {
		contractRepository.deleteById(contractId);
	}
}
