package com.ensolution.ems.client_management.application;

import com.ensolution.ems.client_management.application.command.StackListItem;
import com.ensolution.ems.client_management.application.port.ContractSummary;
import com.ensolution.ems.client_management.application.port.WorkplaceQueryUseCase;
import com.ensolution.ems.client_management.domain.Company;
import com.ensolution.ems.client_management.domain.Stack;
import com.ensolution.ems.client_management.domain.Workplace;
import com.ensolution.ems.client_management.domain.port.CompanyRepository;
import com.ensolution.ems.client_management.domain.port.StackRepository;
import com.ensolution.ems.client_management.domain.port.WorkplaceRepository;
import com.ensolution.ems.global.common.enums.MeasurementField;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkplaceQueryService implements WorkplaceQueryUseCase {
	
	private final WorkplaceRepository workplaceRepository;
	private final CompanyRepository companyRepository;
	private final StackRepository stackRepository;
	
	@Override
	public ContractSummary getSummaryById(Long workplaceId) {
		Workplace workplace = workplaceRepository.findById(workplaceId);
		Company company = companyRepository.findById(workplace.getCompanyId());
		List<StackListItem> stackList = stackRepository.findByWorkplaceId(workplaceId);
		
		List<MeasurementField> fields = stackList.stream()
			.map(StackListItem::field)
			.distinct()
			.toList();
		
		return new ContractSummary(
			fields,
			company.getName(),
			workplace.getName()
		);
	}
	
	@Override
	public Map<Long, List<MeasurementField>> getFieldsByWorkplaceIds(List<Long> workplaceIds) {
		return stackRepository.findFieldsByWorkplaceIds(workplaceIds);
	}

	@Override
	public boolean existsById(Long workplaceId) {
		return workplaceRepository.existsById(workplaceId);
	}
}
