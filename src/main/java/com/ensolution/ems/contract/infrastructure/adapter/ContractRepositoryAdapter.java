package com.ensolution.ems.contract.infrastructure.adapter;

import com.ensolution.ems.contract.application.command.ContractListItem;
import com.ensolution.ems.contract.domain.Contract;
import com.ensolution.ems.contract.domain.port.ContractRepository;
import com.ensolution.ems.contract.infrastructure.mapper.ContractDomainEntityMapper;
import com.ensolution.ems.contract.infrastructure.mapper.ContractListItemMapper;
import com.ensolution.ems.contract.infrastructure.repository.JpaContractRepository;
import com.ensolution.ems.contract.infrastructure.repository.JpaContractTableViewRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional
public class ContractRepositoryAdapter implements ContractRepository {

	private final JpaContractRepository jpaContractRepository;
	private final JpaContractTableViewRepository jpaContractTableViewRepository;
	private final ContractDomainEntityMapper mapper;
	private final ContractListItemMapper contractListItemMapper;

	@Override
	public Contract save(Contract contract) {
		return mapper.toDomain(jpaContractRepository.save(mapper.toEntity(contract)));
	}

	@Override
	public Contract findById(Long id) {
		return jpaContractRepository.findById(id)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
	}

	@Override
	public List<ContractListItem> findByWorkplaceId(Long workplaceId) {
		return contractListItemMapper.toListItems(jpaContractTableViewRepository.findByWorkplaceId(workplaceId));
	}

	@Override
	public List<ContractListItem> findAll() {
		return contractListItemMapper.toListItems(jpaContractTableViewRepository.findAll());
	}

	@Override
	public void deleteById(Long id) {
		jpaContractRepository.deleteById(id);
	}
}
