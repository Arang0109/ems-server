package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.client_management.application.port.out.PollutantRepository;
import com.ensolution.ems.global.common.enums.MeasurementField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PollutantQueryService {

	private final PollutantRepository pollutantRepository;

	public List<Pollutant> getPollutantList(MeasurementField field) {
		if (field == null) return pollutantRepository.findAll();
		return pollutantRepository.findByField(field);
	}

	public Pollutant getPollutant(Long id) {
		return pollutantRepository.findById(id);
	}
}
