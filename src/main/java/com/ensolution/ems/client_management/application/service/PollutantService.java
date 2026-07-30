package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.create.CreatePollutantCommand;
import com.ensolution.ems.client_management.application.command.update.UpdatePollutantCommand;
import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.client_management.application.port.out.PollutantRepository;
import com.ensolution.ems.client_management.application.validator.PollutantValidator;
import com.ensolution.ems.global.common.enums.MeasurementField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PollutantService {

	private final PollutantRepository pollutantRepository;
	private final PollutantValidator pollutantValidator;

	public Pollutant createPollutant(CreatePollutantCommand command) {
		pollutantValidator.requireUniqueNameKr(command.nameKr(), command.tenantId());

		return pollutantRepository.save(Pollutant.register(
			command.tenantId(), command.field(), command.nameKr(), command.nameEn(), command.method(),
			command.phase(), command.equipment(), command.testMethod()
		));
	}

	public Pollutant updatePollutant(Long id, Long tenantId, UpdatePollutantCommand command) {
		Pollutant pollutant = pollutantRepository.findById(id, tenantId);
		return pollutantRepository.save(pollutant.update(
			id, command.field(), command.nameKr(), command.nameEn(),
			command.method(), command.phase(), command.equipment(), command.testMethod()
		));
	}

	public void deletePollutant(Long id, Long tenantId) {
		pollutantRepository.deleteById(id, tenantId);
	}

	@Transactional(readOnly = true)
	public List<Pollutant> getPollutantList(MeasurementField field, Long tenantId) {
		if (field == null) return pollutantRepository.findAll(tenantId);
		return pollutantRepository.findByField(field, tenantId);
	}

	@Transactional(readOnly = true)
	public Pollutant getPollutant(Long id, Long tenantId) {
		return pollutantRepository.findById(id, tenantId);
	}
}
