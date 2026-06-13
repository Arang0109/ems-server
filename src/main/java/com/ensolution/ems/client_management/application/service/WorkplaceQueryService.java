package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.port.in.ContractSummary;
import com.ensolution.ems.client_management.application.port.in.WorkplaceQueryUseCase;
import com.ensolution.ems.client_management.domain.Company;
import com.ensolution.ems.client_management.domain.Workplace;
import com.ensolution.ems.client_management.application.port.out.CompanyRepository;
import com.ensolution.ems.client_management.application.port.out.WorkplaceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkplaceQueryService implements WorkplaceQueryUseCase {
	
	private final WorkplaceRepository workplaceRepository;
	private final CompanyRepository companyRepository;
	
	@Override
	public ContractSummary getSummaryById(Long workplaceId) {
		Workplace workplace = workplaceRepository.findById(workplaceId);
		Company company = companyRepository.findById(workplace.getCompanyId());
		
		return new ContractSummary(
			company.getName(),
			workplace.getName(),
			workplace.getAddress()
		);
	}

	@Override
	public boolean existsById(Long workplaceId) {
		return workplaceRepository.existsById(workplaceId);
	}
}
