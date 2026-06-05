package com.ensolution.ems.client_management.application;

import com.ensolution.ems.client_management.application.command.CreateWorkplaceCommand;
import com.ensolution.ems.client_management.application.command.WorkplaceListItem;
import com.ensolution.ems.client_management.domain.Workplace;
import com.ensolution.ems.client_management.domain.port.WorkplaceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkplaceService {
	
	private final WorkplaceRepository workplaceRepository;
	
	public Workplace createWorkplace(CreateWorkplaceCommand command) {
		Workplace newWorkplace = Workplace.register(
			command.companyId(),
			command.name(),
			command.address(),
			command.bizNumber()
		);
		return workplaceRepository.save(newWorkplace);
	}
	
	public List<WorkplaceListItem> getWorkplaceList(Long companyId) {
		return workplaceRepository.findByCompanyId(companyId);
	}
	
	public Workplace getWorkplace(Long workplaceId) {
		return workplaceRepository.findById(workplaceId);
	}
	
	public void deleteWorkplace(Long workplaceId) { workplaceRepository.deleteById(workplaceId);}
}