package com.ensolution.ems.client_management.infrastructure.adapter;

import com.ensolution.ems.client_management.application.port.out.TargetSubstanceRepository;
import com.ensolution.ems.client_management.domain.TargetSubstance;
import com.ensolution.ems.client_management.infrastructure.entity.TargetSubstanceEntity;
import com.ensolution.ems.client_management.infrastructure.mapper.TargetEntityMapper;
import com.ensolution.ems.client_management.infrastructure.repository.PreventionJpaRepository;
import com.ensolution.ems.client_management.infrastructure.repository.TargetSubstanceJpaRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional
public class TargetSubstanceRepositoryAdapter implements TargetSubstanceRepository {

	private final TargetSubstanceJpaRepository jpaTargetSubstanceRepository;
	private final PreventionJpaRepository jpaPreventionRepository;
	private final TargetEntityMapper mapper;

	@Override
	public TargetSubstance save(TargetSubstance targetSubstance) {
		if (targetSubstance.getId() != null) {
			TargetSubstanceEntity existing = jpaTargetSubstanceRepository.findById(targetSubstance.getId())
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
			TargetSubstanceEntity update = mapper.toEntity(targetSubstance).toBuilder()
				.prevention(existing.getPrevention())
				.build();
			return mapper.toDomain(jpaTargetSubstanceRepository.save(update));
		}
		TargetSubstanceEntity entity = mapper.toEntity(targetSubstance)
			.toBuilder()
			.prevention(jpaPreventionRepository.getReferenceById(targetSubstance.getPreventionId()))
			.build();
		return mapper.toDomain(jpaTargetSubstanceRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public TargetSubstance findById(Long id) {
		return jpaTargetSubstanceRepository.findById(id)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<TargetSubstance> findByPreventionId(Long preventionId) {
		return mapper.toDomainList(jpaTargetSubstanceRepository.findByPreventionId(preventionId));
	}

	@Override
	public void deleteById(Long id) {
		jpaTargetSubstanceRepository.deleteById(id);
	}
}
