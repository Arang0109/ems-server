package com.ensolution.ems.client_management.application;

import com.ensolution.ems.client_management.application.command.AssignStackPollutantCommand;
import com.ensolution.ems.client_management.application.command.StackPollutantListItem;
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

	public StackPollutantListItem assignPollutant(AssignStackPollutantCommand command) {
		if (stackPollutantRepository.existsByStackIdAndPollutantId(command.stackId(), command.pollutantId())) {
			throw new CustomException(ErrorCode.CONFLICT);
		}
		StackPollutant saved = stackPollutantRepository.save(StackPollutant.assign(command.stackId(), command.pollutantId()));
		return stackPollutantRepository.findByStackId(command.stackId())
			.stream()
			.filter(item -> item.id().equals(saved.getId()))
			.findFirst()
			.orElseThrow();
	}

	public List<StackPollutantListItem> getStackPollutantList(Long stackId) {
		return stackPollutantRepository.findByStackId(stackId);
	}

	public void removeStackPollutant(Long id) {
		stackPollutantRepository.deleteById(id);
	}
}
