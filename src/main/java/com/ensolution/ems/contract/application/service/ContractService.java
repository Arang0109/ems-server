package com.ensolution.ems.contract.application.service;

import com.ensolution.ems.client_management.application.port.in.WorkplaceQueryUseCase;
import com.ensolution.ems.contract.application.command.ContractDetail;
import com.ensolution.ems.contract.application.command.ContractListItem;
import com.ensolution.ems.contract.application.command.CreateContractCommand;
import com.ensolution.ems.contract.application.command.UpdateContractCommand;
import com.ensolution.ems.contract.application.mapper.ContractSummaryMapper;
import com.ensolution.ems.contract.application.port.in.ContractQueryUseCase;
import com.ensolution.ems.contract.application.port.in.ContractStatisticsUseCase;
import com.ensolution.ems.contract.application.port.in.ExpiringContractSummary;
import com.ensolution.ems.contract.domain.Contract;
import com.ensolution.ems.contract.domain.port.ContractRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractService implements ContractQueryUseCase, ContractStatisticsUseCase {

	private final ContractRepository contractRepository;
	private final WorkplaceQueryUseCase workplaceQueryUseCase;
	private final ContractDetailAssembler assembler;
	private final ContractSummaryMapper summaryMapper;

	public ContractDetail createContract(CreateContractCommand command) {
		if (!workplaceQueryUseCase.existsById(command.workplaceId(), command.tenantId())) {
			throw new CustomException(ErrorCode.NOT_FOUND);
		}

		Contract saved = contractRepository.save(Contract.register(
			command.tenantId(), command.workplaceId(), command.contractName(), command.contractDate(),
			command.startDate(), command.completionDate(), command.contractAmount(), command.contractAmountUnit(),
			command.vatIncluded(), command.contractGuaranteeAmount(), command.advancePaymentAmount(),
			command.advancePaymentDueDate(), command.delayPenaltyRate(), command.remark()
		));
		return assembler.assemble(saved);
	}

	public ContractDetail updateContract(Long contractId, Long tenantId, UpdateContractCommand command) {
		Contract contract = contractRepository.findById(contractId, tenantId);
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

	public void deleteContract(Long contractId, Long tenantId) {
		contractRepository.deleteById(contractId, tenantId);
	}

	@Override
	public void deleteContracts(Long workplaceId) {
		contractRepository.deleteByWorkplaceId(workplaceId);
	}

	@Transactional(readOnly = true)
	public ContractDetail getContract(Long contractId, Long tenantId) {
		return assembler.assemble(contractRepository.findById(contractId, tenantId));
	}

	@Transactional(readOnly = true)
	public List<ContractListItem> getContractList(Long workplaceId, Long tenantId) {
		return workplaceId == null
			? contractRepository.findAllByTenantId(tenantId)
			: contractRepository.findByWorkplaceId(workplaceId, tenantId);
	}

	@Override
	@Transactional(readOnly = true)
	public long countContracts(Long tenantId) {
		return contractRepository.findAllByTenantId(tenantId).size();
	}

	@Override
	@Transactional(readOnly = true)
	public long countContractsInMonth(Long tenantId, YearMonth yearMonth) {
		return contractRepository.findAllByTenantId(tenantId).stream()
			.map(ContractListItem::contractDate)
			.filter(contractDate -> contractDate != null && yearMonth.equals(YearMonth.from(contractDate)))
			.count();
	}

	@Override
	@Transactional(readOnly = true)
	public List<ExpiringContractSummary> findExpiringBetween(Long tenantId, LocalDate from, LocalDate to) {
		return summaryMapper.toExpiringSummaries(
			contractRepository.findAllByTenantId(tenantId).stream()
				.filter(item -> isWithin(item.completionDate(), from, to))
				.sorted(Comparator.comparing(ContractListItem::completionDate))
				.toList()
		);
	}

	private static boolean isWithin(LocalDate completionDate, LocalDate from, LocalDate to) {
		return completionDate != null && !completionDate.isBefore(from) && !completionDate.isAfter(to);
	}
}
