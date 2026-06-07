package com.ensolution.ems.client_management.infrastructure.adapter;

import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.client_management.domain.port.PollutantRepository;
import com.ensolution.ems.client_management.infrastructure.mapper.PollutantDomainEntityMapper;
import com.ensolution.ems.client_management.infrastructure.repository.JpaPollutantRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional
public class PollutantRepositoryAdapter implements PollutantRepository {

	private final JpaPollutantRepository jpaPollutantRepository;
	private final PollutantDomainEntityMapper mapper;

	@Override
	public Pollutant save(Pollutant pollutant) {
		return mapper.toDomain(jpaPollutantRepository.save(mapper.toEntity(pollutant)));
	}

	@Override
	@Transactional(readOnly = true)
	public Pollutant findById(Long id) {
		return jpaPollutantRepository.findById(id)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Pollutant> findAll() {
		return mapper.toDomainList(jpaPollutantRepository.findAll());
	}

	@Override
	public void deleteById(Long id) {
		jpaPollutantRepository.deleteById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByNameKr(String nameKr) {
		return jpaPollutantRepository.existsByNameKr(nameKr);
	}
}
