package com.ensolution.ems.client_management.application;

import com.ensolution.ems.client_management.application.command.CreateWorkplaceCommand;
import com.ensolution.ems.client_management.application.command.UpdateWorkplaceCommand;
import com.ensolution.ems.client_management.application.command.WorkplaceListItem;
import com.ensolution.ems.client_management.domain.Workplace;
import com.ensolution.ems.client_management.domain.port.WorkplaceRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
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
		
		Long companyId = command.companyId();
		String name = command.name();
		
		if (workplaceRepository.existsByNameAndCompanyId(name, companyId)) {
			throw new CustomException(ErrorCode.CONFLICT);
		}
		
		Workplace newWorkplace = Workplace.register(companyId, name, command.address(), command.bizNumber());
		return workplaceRepository.save(newWorkplace);
	}
	
	public List<WorkplaceListItem> getWorkplaceList(Long companyId) {
		return workplaceRepository.findByCompanyId(companyId);
	}
	
	public Workplace getWorkplace(Long workplaceId) {
		return workplaceRepository.findById(workplaceId);
	}

	public Workplace updateWorkplace(Long workplaceId, UpdateWorkplaceCommand command) {
		Workplace workplace = workplaceRepository.findById(workplaceId);
		Workplace updated = workplace.update(workplaceId, command.name(), command.address(), command.bizNumber());
		return workplaceRepository.save(updated);
	}

	public void deleteWorkplace(Long workplaceId) { workplaceRepository.deleteById(workplaceId); }
}