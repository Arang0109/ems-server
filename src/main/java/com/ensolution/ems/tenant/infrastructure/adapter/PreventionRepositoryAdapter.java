package com.ensolution.ems.tenant.infrastructure.adapter;

import com.ensolution.ems.tenant.application.port.out.PreventionRepository;
import com.ensolution.ems.tenant.domain.Prevention;
import com.ensolution.ems.tenant.infrastructure.entity.PreventionEntity;
import com.ensolution.ems.tenant.infrastructure.mapper.PreventionEntityMapper;
import com.ensolution.ems.tenant.infrastructure.repository.PreventionJpaRepository;
import com.ensolution.ems.tenant.infrastructure.repository.StackJpaRepository;
import com.ensolution.ems.tenant.infrastructure.repository.TenantJpaRepository;
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
	private final TenantJpaRepository jpaTenantRepository;
	private final PreventionEntityMapper mapper;

	@Override
	public Prevention save(Prevention prevention) {
		if (prevention.getId() != null) {
			PreventionEntity existing = jpaPreventionRepository.findByPreventionIdAndTenant_TenantId(prevention.getId(), prevention.getTenantId())
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
			PreventionEntity update = mapper.toEntity(prevention).toBuilder()
				.tenant(existing.getTenant())
				.stack(existing.getStack())
				.targetSubstances(existing.getTargetSubstances())
				.build();
			return mapper.toDomain(jpaPreventionRepository.save(update));
		}
		PreventionEntity entity = mapper.toEntity(prevention)
			.toBuilder()
			.tenant(jpaTenantRepository.getReferenceById(prevention.getTenantId()))
			.stack(jpaStackRepository.getReferenceById(prevention.getStackId()))
			.build();
		return mapper.toDomain(jpaPreventionRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public Prevention findById(Long id, Long tenantId) {
		return jpaPreventionRepository.findByPreventionIdAndTenant_TenantId(id, tenantId)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Prevention> findByStackId(Long stackId, Long tenantId) {
		return mapper.toDomainList(jpaPreventionRepository.findByStack_StackIdAndTenant_TenantId(stackId, tenantId));
	}

	@Override
	public void deleteById(Long id, Long tenantId) {
		int deletedCount = jpaPreventionRepository.deleteByPreventionIdAndTenantId(id, tenantId);

		if (deletedCount == 0) {
			throw new CustomException(ErrorCode.NOT_FOUND);
		}
	}
}
