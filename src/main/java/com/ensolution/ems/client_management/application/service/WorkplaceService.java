package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.create.CreateWorkplaceCommand;
import com.ensolution.ems.client_management.application.command.list_item.WorkplaceListItem;
import com.ensolution.ems.client_management.application.command.update.UpdateWorkplaceCommand;
import com.ensolution.ems.client_management.application.port.in.ContractSummary;
import com.ensolution.ems.client_management.application.port.in.WorkplaceQueryUseCase;
import com.ensolution.ems.client_management.application.port.out.ClientRepository;
import com.ensolution.ems.client_management.application.port.out.WorkplaceRepository;
import com.ensolution.ems.client_management.domain.Client;
import com.ensolution.ems.client_management.domain.Workplace;
import com.ensolution.ems.contract.application.port.ContractQueryUseCase;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkplaceService implements WorkplaceQueryUseCase {

	private final WorkplaceRepository workplaceRepository;
	private final ClientRepository clientRepository;
	private final ContractQueryUseCase contractQueryUseCase;

	public Workplace createWorkplace(CreateWorkplaceCommand command) {

		Long clientId = command.clientId();
		String name = command.name();

		if (workplaceRepository.existsByNameAndClientId(name, clientId)) {
			throw new CustomException(ErrorCode.CONFLICT);
		}

		Workplace newWorkplace = Workplace.register(
			clientId, name, command.zipcode(), command.roadAddress(), command.address(), command.bizNumber(), command.grade()
		);
		return workplaceRepository.save(newWorkplace);
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

	@Transactional(readOnly = true)
	public List<WorkplaceListItem> getWorkplaceList(Long clientId) {
		if (clientId == null) return workplaceRepository.findAll();
		return workplaceRepository.findByClientId(clientId);
	}

	@Transactional(readOnly = true)
	public Workplace getWorkplace(Long workplaceId) {
		return workplaceRepository.findById(workplaceId);
	}

	@Override
	@Transactional(readOnly = true)
	public ContractSummary getSummaryById(Long workplaceId) {
		Workplace workplace = workplaceRepository.findById(workplaceId);
		Client client = clientRepository.findById(workplace.getClientId());

		return new ContractSummary(
			client.getName(),
			workplace.getName(),
			workplace.getAddress()
		);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsById(Long workplaceId) {
		return workplaceRepository.existsById(workplaceId);
	}
}
