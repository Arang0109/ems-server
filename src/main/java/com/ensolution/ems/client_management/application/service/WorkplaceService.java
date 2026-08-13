package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.create.CreateWorkplaceCommand;
import com.ensolution.ems.client_management.application.command.list_item.WorkplaceListItem;
import com.ensolution.ems.client_management.application.command.update.UpdateWorkplaceCommand;
import com.ensolution.ems.client_management.application.event.WorkplaceDeletedEvent;
import com.ensolution.ems.client_management.application.port.in.ContractSummary;
import com.ensolution.ems.client_management.application.port.in.WorkplaceQueryUseCase;
import com.ensolution.ems.client_management.application.port.out.ClientRepository;
import com.ensolution.ems.client_management.application.port.out.WorkplaceRepository;
import com.ensolution.ems.client_management.application.validator.WorkplaceValidator;
import com.ensolution.ems.client_management.domain.Client;
import com.ensolution.ems.client_management.domain.Workplace;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkplaceService implements WorkplaceQueryUseCase {

	private final WorkplaceRepository workplaceRepository;
	private final ClientRepository clientRepository;
	private final WorkplaceValidator workplaceValidator;
	private final ApplicationEventPublisher eventPublisher;

	public Workplace createWorkplace(CreateWorkplaceCommand command) {

		Long clientId = command.clientId();
		String name = command.name();

		workplaceValidator.requireUniqueNameInClient(name, clientId);

		Workplace newWorkplace = Workplace.register(
			command.tenantId(),
			clientId, name,
			command.bizNumber(),
			command.businessCategory(),
			command.roadAddress(),
			command.detailAddress(),
			command.zipcode(),
			command.facilityManager(),
			command.samplingWitness(),
			command.grade()
		);
		return workplaceRepository.save(newWorkplace);
	}

	public Workplace updateWorkplace(Long workplaceId, Long tenantId, UpdateWorkplaceCommand command) {
		Workplace workplace = workplaceRepository.findById(workplaceId, tenantId);
		Workplace updated = workplace.update(
			command.name(),
			command.bizNumber(),
			command.businessCategory(),
			command.roadAddress(),
			command.detailAddress(), command.zipcode(),
			command.facilityManager(), command.samplingWitness(), command.grade()
		);
		return workplaceRepository.save(updated);
	}

	public void deleteWorkplace(Long workplaceId, Long tenantId) {
		workplaceRepository.deleteById(workplaceId, tenantId);
		eventPublisher.publishEvent(new WorkplaceDeletedEvent(workplaceId));
	}

	@Transactional(readOnly = true)
	public List<WorkplaceListItem> getWorkplaceList(Long clientId, Long tenantId) {
		if (clientId == null) return workplaceRepository.findAll(tenantId);
		return workplaceRepository.findByClientId(clientId, tenantId);
	}

	@Transactional(readOnly = true)
	public Workplace getWorkplace(Long workplaceId, Long tenantId) {
		return workplaceRepository.findById(workplaceId, tenantId);
	}

	@Override
	@Transactional(readOnly = true)
	public ContractSummary getSummaryById(Long workplaceId, Long tenantId) {
		Workplace workplace = workplaceRepository.findById(workplaceId, tenantId);
		Client client = clientRepository.findById(workplace.getClientId(), tenantId);
		
		String address = workplace.getRoadAddress() + " " + workplace.getDetailAddress();

		return new ContractSummary(
			client.getName(),
			workplace.getName(),
			address
		);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsById(Long workplaceId) {
		return workplaceRepository.existsById(workplaceId);
	}

	@Override
	@Transactional(readOnly = true)
	public long countWorkplaces(Long tenantId) {
		return workplaceRepository.findAll(tenantId).size();
	}
}
