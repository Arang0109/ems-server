package com.ensolution.ems.client_management.application;

import com.ensolution.ems.client_management.application.command.create.CreateStackPollutantCommand;
import com.ensolution.ems.client_management.application.command.list_item.StackPollutantListItem;
import com.ensolution.ems.client_management.domain.StackPollutant;
import com.ensolution.ems.client_management.domain.port.StackPollutantRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
			StackPollutant.register(command.stackId(), command.pollutantId(), command.cycle(), command.allowance()));
	}

	public List<StackPollutantListItem> getStackPollutantList(Long stackId) {
		return stackPollutantRepository.findByStackId(stackId);
	}

	public void removeStackPollutant(Long id) {
		stackPollutantRepository.deleteById(id);
	}
}
