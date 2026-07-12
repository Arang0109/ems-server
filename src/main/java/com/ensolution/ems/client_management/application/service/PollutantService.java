package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.create.CreatePollutantCommand;
import com.ensolution.ems.client_management.application.command.update.UpdatePollutantCommand;
import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.client_management.application.port.out.PollutantRepository;
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
		if (pollutantRepository.existsByNameKr(command.nameKr())) {
			throw new CustomException(ErrorCode.CONFLICT);
		}
		return pollutantRepository.save(Pollutant.register(
			command.field(), command.nameKr(), command.nameEn(), command.method(),
			command.phase(), command.equipment(), command.testMethod()
		));
	}

	public Pollutant updatePollutant(Long id, UpdatePollutantCommand command) {
		Pollutant pollutant = pollutantRepository.findById(id);
		return pollutantRepository.save(pollutant.update(
			id, command.field(), command.nameKr(), command.nameEn(),
			command.method(), command.phase(), command.equipment(), command.testMethod()
		));
	}

	public void deletePollutant(Long id) {
		pollutantRepository.deleteById(id);
	}

	@Transactional(readOnly = true)
	public List<Pollutant> getPollutantList(MeasurementField field) {
		if (field == null) return pollutantRepository.findAll();
		return pollutantRepository.findByField(field);
	}

	@Transactional(readOnly = true)
	public Pollutant getPollutant(Long id) {
		return pollutantRepository.findById(id);
	}
}
