package com.ensolution.ems.tenant.application.service;

import com.ensolution.ems.tenant.application.command.create.CreateStackPollutantCommand;
import com.ensolution.ems.tenant.application.command.list_item.StackPollutantListItem;
import com.ensolution.ems.tenant.domain.StackPollutant;
import com.ensolution.ems.tenant.application.port.out.StackPollutantRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class StackPollutantService {

	private final StackPollutantRepository stackPollutantRepository;

	public StackPollutant createStackPollutant(CreateStackPollutantCommand command) {
		if (stackPollutantRepository.existsByStackIdAndPollutantId(command.stackId(), command.pollutantId())) {
			throw new CustomException(ErrorCode.CONFLICT);
		}
		return stackPollutantRepository.save(
			StackPollutant.register(command.tenantId(), command.stackId(), command.pollutantId(), command.cycle(), command.allowance()));
	}

	public List<StackPollutant> createStackPollutants(List<CreateStackPollutantCommand> commands) {
		Set<String> seen = new HashSet<>();
		for (CreateStackPollutantCommand command : commands) {
			if (!seen.add(command.stackId() + ":" + command.pollutantId())) {
				throw new CustomException(ErrorCode.CONFLICT);
			}
		}
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
