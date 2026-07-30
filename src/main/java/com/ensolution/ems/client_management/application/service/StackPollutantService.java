package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.create.CreateStackPollutantCommand;
import com.ensolution.ems.client_management.application.command.list_item.StackPollutantListItem;
import com.ensolution.ems.client_management.domain.StackPollutant;
import com.ensolution.ems.client_management.application.port.out.StackPollutantRepository;
import com.ensolution.ems.client_management.application.validator.StackPollutantValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StackPollutantService {

	private final StackPollutantRepository stackPollutantRepository;
	private final StackPollutantValidator stackPollutantValidator;

	public StackPollutant createStackPollutant(CreateStackPollutantCommand command) {
		stackPollutantValidator.requireNotRegistered(command.stackId(), command.pollutantId());

		return stackPollutantRepository.save(
			StackPollutant.register(command.tenantId(), command.stackId(), command.pollutantId(), command.cycle(), command.allowance()));
	}

	public List<StackPollutant> createStackPollutants(List<CreateStackPollutantCommand> commands) {
		stackPollutantValidator.requireNoDuplicatesInBatch(commands);

		return commands.stream().map(this::createStackPollutant).toList();
	}

	public void removeStackPollutant(Long id, Long tenantId) {
		stackPollutantRepository.deleteById(id, tenantId);
	}

	@Transactional(readOnly = true)
	public List<StackPollutantListItem> getStackPollutantList(Long stackId, Long tenantId) {
		return stackPollutantRepository.findByStackId(stackId, tenantId);
	}
}
