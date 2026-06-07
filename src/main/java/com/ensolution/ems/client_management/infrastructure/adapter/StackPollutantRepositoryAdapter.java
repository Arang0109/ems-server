package com.ensolution.ems.client_management.infrastructure.adapter;

import com.ensolution.ems.client_management.application.command.StackPollutantListItem;
import com.ensolution.ems.client_management.domain.StackPollutant;
import com.ensolution.ems.client_management.domain.port.StackPollutantRepository;
import com.ensolution.ems.client_management.infrastructure.entity.JpaStackPollutantEntity;
import com.ensolution.ems.client_management.infrastructure.mapper.StackPollutantDomainEntityMapper;
import com.ensolution.ems.client_management.infrastructure.repository.JpaPollutantRepository;
import com.ensolution.ems.client_management.infrastructure.repository.JpaStackPollutantRepository;
import com.ensolution.ems.client_management.infrastructure.repository.JpaStackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional
public class StackPollutantRepositoryAdapter implements StackPollutantRepository {

	private final JpaStackPollutantRepository jpaStackPollutantRepository;
	private final JpaStackRepository jpaStackRepository;
	private final JpaPollutantRepository jpaPollutantRepository;
	private final StackPollutantDomainEntityMapper mapper;

	@Override
	public StackPollutant save(StackPollutant stackPollutant) {
		JpaStackPollutantEntity entity = mapper.toEntity(stackPollutant)
			.toBuilder()
			.stack(jpaStackRepository.getReferenceById(stackPollutant.getStackId()))
			.pollutant(jpaPollutantRepository.getReferenceById(stackPollutant.getPollutantId()))
			.build();
		return mapper.toDomain(jpaStackPollutantRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public List<StackPollutantListItem> findByStackId(Long stackId) {
		return mapper.toListItems(jpaStackPollutantRepository.findByStackId(stackId));
	}

	@Override
	public void deleteById(Long id) {
		jpaStackPollutantRepository.deleteById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByStackIdAndPollutantId(Long stackId, Long pollutantId) {
		return jpaStackPollutantRepository.existsByStackIdAndPollutantId(stackId, pollutantId);
	}
}
