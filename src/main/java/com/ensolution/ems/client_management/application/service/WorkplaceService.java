package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.create.CreateWorkplaceCommand;
import com.ensolution.ems.client_management.application.command.update.UpdateWorkplaceCommand;
import com.ensolution.ems.client_management.application.command.list_item.WorkplaceListItem;
import com.ensolution.ems.client_management.domain.Workplace;
import com.ensolution.ems.client_management.application.port.out.WorkplaceRepository;
import com.ensolution.ems.contract.application.port.ContractQueryUseCase;
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
	private final ContractQueryUseCase contractQueryUseCase;
	
	public Workplace createWorkplace(CreateWorkplaceCommand command) {
		
		Long companyId = command.companyId();
		String name = command.name();
		
		if (workplaceRepository.existsByNameAndCompanyId(name, companyId)) {
			throw new CustomException(ErrorCode.CONFLICT);
		}
		
		Workplace newWorkplace = Workplace.register(
			companyId, name, command.zipcode(), command.roadAddress(), command.address(), command.bizNumber(), command.grade()
		);
		return workplaceRepository.save(newWorkplace);
	}
	
	public List<WorkplaceListItem> getWorkplaceList(Long companyId) {
		if (companyId == null) return workplaceRepository.findAll();
		return workplaceRepository.findByCompanyId(companyId);
	}
	
	public Workplace getWorkplace(Long workplaceId) {
		return workplaceRepository.findById(workplaceId);
	}

	public Workplace updateWorkplace(Long workplaceId, UpdateWorkplaceCommand command) {
		Workplace workplace = workplaceRepository.findById(workplaceId);
		Workplace updated = workplace.update(
			command.name(), command.zipcode(), command.roadAddress(), command.address(), command.bizNumber(), command.grade()
		);
		return workplaceRepository.save(updated);
	}

	public void deleteWorkplace(Long workplaceId) {
		workplaceRepository.deleteById(workplaceId);
		contractQueryUseCase.deleteContracts(workplaceId);
	}
}