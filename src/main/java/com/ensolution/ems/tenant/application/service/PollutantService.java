package com.ensolution.ems.tenant.application.service;

import com.ensolution.ems.tenant.application.command.create.CreatePollutantCommand;
import com.ensolution.ems.tenant.application.command.update.UpdatePollutantCommand;
import com.ensolution.ems.tenant.domain.Pollutant;
import com.ensolution.ems.tenant.application.port.out.PollutantRepository;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PollutantService {

	private final PollutantRepository pollutantRepository;

	public Pollutant createPollutant(CreatePollutantCommand command) {
		if (pollutantRepository.existsByNameKrAndTenantId(command.nameKr(), command.tenantId())) {
			throw new CustomException(ErrorCode.CONFLICT);
		}
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
