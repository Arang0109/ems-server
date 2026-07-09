package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.list_item.WorkplaceListItem;
import com.ensolution.ems.client_management.application.port.in.ContractSummary;
import com.ensolution.ems.client_management.application.port.in.WorkplaceQueryUseCase;
import com.ensolution.ems.client_management.domain.Company;
import com.ensolution.ems.client_management.domain.Workplace;
import com.ensolution.ems.client_management.application.port.out.CompanyRepository;
import com.ensolution.ems.client_management.application.port.out.WorkplaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkplaceQueryService implements WorkplaceQueryUseCase {

	private final WorkplaceRepository workplaceRepository;
	private final CompanyRepository companyRepository;

	public List<WorkplaceListItem> getWorkplaceList(Long companyId) {
		if (companyId == null) return workplaceRepository.findAll();
		return workplaceRepository.findByCompanyId(companyId);
	}

	public Workplace getWorkplace(Long workplaceId) {
		return workplaceRepository.findById(workplaceId);
	}

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
