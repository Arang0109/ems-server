package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.create.CreateStackPollutantCommand;
import com.ensolution.ems.client_management.domain.StackPollutant;
import com.ensolution.ems.client_management.application.port.out.StackPollutantRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StackPollutantCommandService {

	private final StackPollutantRepository stackPollutantRepository;

	public StackPollutant createStackPollutant(CreateStackPollutantCommand command) {
		if (stackPollutantRepository.existsByStackIdAndPollutantId(command.stackId(), command.pollutantId())) {
			throw new CustomException(ErrorCode.CONFLICT);
		}
		return stackPollutantRepository.save(
			StackPollutant.register(command.stackId(), command.pollutantId(), command.cycle(), command.allowance()));
	}

	public void removeStackPollutant(Long id) {
		stackPollutantRepository.deleteById(id);
	}
}
