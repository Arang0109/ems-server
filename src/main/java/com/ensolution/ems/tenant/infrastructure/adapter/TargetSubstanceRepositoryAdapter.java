package com.ensolution.ems.tenant.infrastructure.adapter;

import com.ensolution.ems.tenant.application.port.out.TargetSubstanceRepository;
import com.ensolution.ems.tenant.domain.TargetSubstance;
import com.ensolution.ems.tenant.infrastructure.entity.TargetSubstanceEntity;
import com.ensolution.ems.tenant.infrastructure.mapper.TargetSubstanceEntityMapper;
import com.ensolution.ems.tenant.infrastructure.repository.PreventionJpaRepository;
import com.ensolution.ems.tenant.infrastructure.repository.TargetSubstanceJpaRepository;
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
public class TargetSubstanceRepositoryAdapter implements TargetSubstanceRepository {

	private final TargetSubstanceJpaRepository jpaTargetSubstanceRepository;
	private final PreventionJpaRepository jpaPreventionRepository;
	private final TenantJpaRepository jpaTenantRepository;
	private final TargetSubstanceEntityMapper mapper;

	@Override
	public TargetSubstance save(TargetSubstance targetSubstance) {
		if (targetSubstance.getId() != null) {
			TargetSubstanceEntity existing = jpaTargetSubstanceRepository.findByTargetSubstanceIdAndTenant_TenantId(targetSubstance.getId(), targetSubstance.getTenantId())
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
			TargetSubstanceEntity update = mapper.toEntity(targetSubstance).toBuilder()
				.prevention(existing.getPrevention())
				.build();
			return mapper.toDomain(jpaTargetSubstanceRepository.save(update));
		}
		TargetSubstanceEntity entity = mapper.toEntity(targetSubstance)
			.toBuilder()
			.tenant(jpaTenantRepository.getReferenceById(targetSubstance.getTenantId()))
			.prevention(jpaPreventionRepository.getReferenceById(targetSubstance.getPreventionId()))
			.build();
		return mapper.toDomain(jpaTargetSubstanceRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public TargetSubstance findById(Long id, Long tenantId) {
		return jpaTargetSubstanceRepository.findByTargetSubstanceIdAndTenant_TenantId(id, tenantId)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<TargetSubstance> findByPreventionId(Long preventionId, Long tenantId) {
		return mapper.toDomainList(jpaTargetSubstanceRepository.findByPrevention_PreventionIdAndTenant_TenantId(preventionId, tenantId));
	}

	@Override
	public void deleteById(Long id, Long tenantId) {
		int deletedCount = jpaTargetSubstanceRepository.deleteByTargetSubstanceIdAndTenantId(id, tenantId);

		if (deletedCount == 0) {
			throw new CustomException(ErrorCode.NOT_FOUND);
		}
	}
}
