package com.ensolution.ems.tenant.application.service;

import com.ensolution.ems.tenant.application.command.create.CreateWorkplaceCommand;
import com.ensolution.ems.tenant.application.command.list_item.WorkplaceListItem;
import com.ensolution.ems.tenant.application.command.update.UpdateWorkplaceCommand;
import com.ensolution.ems.tenant.application.event.WorkplaceDeletedEvent;
import com.ensolution.ems.tenant.application.port.in.ContractSummary;
import com.ensolution.ems.tenant.application.port.in.WorkplaceQueryUseCase;
import com.ensolution.ems.tenant.application.port.out.ClientRepository;
import com.ensolution.ems.tenant.application.port.out.WorkplaceRepository;
import com.ensolution.ems.tenant.domain.Client;
import com.ensolution.ems.tenant.domain.Workplace;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
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
	private final ApplicationEventPublisher eventPublisher;

	public Workplace createWorkplace(CreateWorkplaceCommand command) {

		Long clientId = command.clientId();
		String name = command.name();

		if (workplaceRepository.existsByNameAndClientId(name, clientId)) {
			throw new CustomException(ErrorCode.CONFLICT);
		}

		Workplace newWorkplace = Workplace.register(
			command.tenantId(), clientId, name, command.bizNumber(), command.roadAddress(), command.detailAddress(), command.zipcode(), command.facilityManager(), command.samplingWitness(), command.grade()
		);
		return workplaceRepository.save(newWorkplace);
	}

	public Workplace updateWorkplace(Long workplaceId, Long tenantId, UpdateWorkplaceCommand command) {
		Workplace workplace = workplaceRepository.findById(workplaceId, tenantId);
		Workplace updated = workplace.update(
			command.name(), command.bizNumber(), command.roadAddress(), command.detailAddress(), command.zipcode(), command.facilityManager(), command.samplingWitness(), command.grade()
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
