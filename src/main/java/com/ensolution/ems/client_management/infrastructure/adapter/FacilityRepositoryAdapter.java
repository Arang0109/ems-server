package com.ensolution.ems.client_management.infrastructure.adapter;

import com.ensolution.ems.client_management.application.port.out.FacilityRepository;
import com.ensolution.ems.client_management.domain.Facility;
import com.ensolution.ems.client_management.infrastructure.entity.FacilityEntity;
import com.ensolution.ems.client_management.infrastructure.mapper.FacilityEntityMapper;
import com.ensolution.ems.client_management.infrastructure.repository.FacilityJpaRepository;
import com.ensolution.ems.client_management.infrastructure.repository.StackJpaRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional
public class FacilityRepositoryAdapter implements FacilityRepository {

	private final FacilityJpaRepository jpaFacilityRepository;
	private final StackJpaRepository jpaStackRepository;
	private final FacilityEntityMapper mapper;

	@Override
	public Facility save(Facility facility) {
		if (facility.getId() != null) {
			FacilityEntity existing = jpaFacilityRepository.findById(facility.getId())
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
			FacilityEntity update = mapper.toEntity(facility).toBuilder()
				.stack(existing.getStack())
				.build();
			return mapper.toDomain(jpaFacilityRepository.save(update));
		}
		FacilityEntity entity = mapper.toEntity(facility)
			.toBuilder()
			.stack(jpaStackRepository.getReferenceById(facility.getStackId()))
			.build();
		return mapper.toDomain(jpaFacilityRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public Facility findById(Long id) {
		return jpaFacilityRepository.findById(id)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Facility> findByStackId(Long stackId) {
		return mapper.toDomainList(jpaFacilityRepository.findByStackId(stackId));
	}

	@Override
	public void deleteById(Long id) {
		jpaFacilityRepository.deleteById(id);
	}
}
