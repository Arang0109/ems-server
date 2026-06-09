package com.ensolution.ems.contract.application;

import com.ensolution.ems.contract.application.port.ContractQueryUseCase;
import com.ensolution.ems.contract.domain.port.ContractRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractQueryService implements ContractQueryUseCase {
	
	private final ContractRepository contractRepository;
	
	@Override
	public void deleteContracts(Long workplaceId) {
		contractRepository.deleteByWorkplaceId(workplaceId);
	}
}
