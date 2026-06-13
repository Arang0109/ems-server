package com.ensolution.ems.client_management.infrastructure.adapter;

import com.ensolution.ems.client_management.application.port.out.PreventionRepository;
import com.ensolution.ems.client_management.domain.Prevention;
import com.ensolution.ems.client_management.infrastructure.entity.PreventionEntity;
import com.ensolution.ems.client_management.infrastructure.mapper.PreventionEntityMapper;
import com.ensolution.ems.client_management.infrastructure.repository.PreventionJpaRepository;
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
public class PreventionRepositoryAdapter implements PreventionRepository {

	private final PreventionJpaRepository jpaPreventionRepository;
	private final StackJpaRepository jpaStackRepository;
	private final PreventionEntityMapper mapper;

	@Override
	public Prevention save(Prevention prevention) {
		if (prevention.getId() != null) {
			PreventionEntity existing = jpaPreventionRepository.findById(prevention.getId())
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
			PreventionEntity update = mapper.toEntity(prevention).toBuilder()
				.stack(existing.getStack())
				.targets(existing.getTargets())
				.build();
			return mapper.toDomain(jpaPreventionRepository.save(update));
		}
		PreventionEntity entity = mapper.toEntity(prevention)
			.toBuilder()
			.stack(jpaStackRepository.getReferenceById(prevention.getStackId()))
			.build();
		return mapper.toDomain(jpaPreventionRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public Prevention findById(Long id) {
		return jpaPreventionRepository.findById(id)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Prevention> findByStackId(Long stackId) {
		return mapper.toDomainList(jpaPreventionRepository.findByStackId(stackId));
	}

	@Override
	public void deleteById(Long id) {
		jpaPreventionRepository.deleteById(id);
	}
}
