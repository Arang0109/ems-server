package com.ensolution.ems.client_management.application;

import com.ensolution.ems.client_management.application.command.CreatePollutantCommand;
import com.ensolution.ems.client_management.application.command.UpdatePollutantCommand;
import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.client_management.domain.port.PollutantRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
		return pollutantRepository.save(Pollutant.register(command.nameKr(), command.nameEn()));
	}

	public List<Pollutant> getPollutantList() {
		return pollutantRepository.findAll();
	}

	public Pollutant getPollutant(Long id) {
		return pollutantRepository.findById(id);
	}

	public Pollutant updatePollutant(Long id, UpdatePollutantCommand command) {
		Pollutant pollutant = pollutantRepository.findById(id);
		return pollutantRepository.save(pollutant.update(command.nameKr(), command.nameEn()));
	}

	public void deletePollutant(Long id) {
		pollutantRepository.deleteById(id);
	}
}
