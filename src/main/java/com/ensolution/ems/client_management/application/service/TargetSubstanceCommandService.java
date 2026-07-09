package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.create.CreateTargetSubstanceCommand;
import com.ensolution.ems.client_management.application.command.update.UpdateTargetSubstanceCommand;
import com.ensolution.ems.client_management.application.port.out.PreventionRepository;
import com.ensolution.ems.client_management.application.port.out.TargetSubstanceRepository;
import com.ensolution.ems.client_management.domain.TargetSubstance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TargetSubstanceCommandService {

	private final TargetSubstanceRepository targetSubstanceRepository;
	private final PreventionRepository preventionRepository;

	public TargetSubstance createTargetSubstance(CreateTargetSubstanceCommand command) {
		preventionRepository.findById(command.preventionId());
		return targetSubstanceRepository.save(
			TargetSubstance.register(command.preventionId(), command.name(), command.removalEfficiency())
		);
	}

	public TargetSubstance updateTargetSubstance(Long targetSubstanceId, UpdateTargetSubstanceCommand command) {
		TargetSubstance targetSubstance = targetSubstanceRepository.findById(targetSubstanceId);
		return targetSubstanceRepository.save(
			targetSubstance.update(command.name(), command.removalEfficiency())
		);
	}

	public void deleteTargetSubstance(Long targetSubstanceId) {
		targetSubstanceRepository.deleteById(targetSubstanceId);
	}
}
