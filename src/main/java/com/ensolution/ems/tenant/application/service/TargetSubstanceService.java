package com.ensolution.ems.tenant.application.service;

import com.ensolution.ems.tenant.application.command.create.CreateTargetSubstanceCommand;
import com.ensolution.ems.tenant.application.command.update.UpdateTargetSubstanceCommand;
import com.ensolution.ems.tenant.application.port.out.PreventionRepository;
import com.ensolution.ems.tenant.application.port.out.TargetSubstanceRepository;
import com.ensolution.ems.tenant.domain.TargetSubstance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TargetSubstanceService {

	private final TargetSubstanceRepository targetSubstanceRepository;
	private final PreventionRepository preventionRepository;

	public TargetSubstance createTargetSubstance(CreateTargetSubstanceCommand command) {
		preventionRepository.findById(command.preventionId(), command.tenantId());
		return targetSubstanceRepository.save(
			TargetSubstance.register(command.tenantId(), command.preventionId(), command.name(), command.removalEfficiency())
		);
	}

	public TargetSubstance updateTargetSubstance(Long targetSubstanceId, Long tenantId, UpdateTargetSubstanceCommand command) {
		TargetSubstance targetSubstance = targetSubstanceRepository.findById(targetSubstanceId, tenantId);
		return targetSubstanceRepository.save(
			targetSubstance.update(command.name(), command.removalEfficiency())
		);
	}

	public void deleteTargetSubstance(Long targetSubstanceId, Long tenantId) {
		targetSubstanceRepository.deleteById(targetSubstanceId, tenantId);
	}

	@Transactional(readOnly = true)
	public TargetSubstance getTargetSubstance(Long targetSubstanceId, Long tenantId) {
		return targetSubstanceRepository.findById(targetSubstanceId, tenantId);
	}

	@Transactional(readOnly = true)
	public List<TargetSubstance> getTargetSubstanceList(Long preventionId, Long tenantId) {
		return targetSubstanceRepository.findByPreventionId(preventionId, tenantId);
	}
}
