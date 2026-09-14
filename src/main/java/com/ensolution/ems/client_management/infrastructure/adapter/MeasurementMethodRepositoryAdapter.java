package com.ensolution.ems.client_management.infrastructure.adapter;

import com.ensolution.ems.client_management.application.port.out.MeasurementMethodRepository;
import com.ensolution.ems.client_management.domain.MeasurementMethod;
import com.ensolution.ems.client_management.infrastructure.entity.MeasurementMethodEntity;
import com.ensolution.ems.client_management.infrastructure.mapper.MeasurementMethodEntityMapper;
import com.ensolution.ems.client_management.infrastructure.repository.MeasurementMethodJpaRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.platform.infrastructure.repository.TenantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional
public class MeasurementMethodRepositoryAdapter implements MeasurementMethodRepository {

	private final MeasurementMethodJpaRepository jpaMethodRepository;
	private final TenantJpaRepository jpaTenantRepository;
	private final MeasurementMethodEntityMapper mapper;

	@Override
	public MeasurementMethod save(MeasurementMethod method) {
		if (method.getId() != null) {
			jpaMethodRepository.findById(method.getId())
				.orElseThrow(() -> new CustomException(ErrorCode.MEASUREMENT_METHOD_NOT_FOUND));
		}
		MeasurementMethodEntity entity = mapper.toEntity(method).toBuilder()
			.tenant(jpaTenantRepository.getReferenceById(method.getTenantId()))
			.build();
		return mapper.toDomain(jpaMethodRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public MeasurementMethod findById(Long id, Long tenantId) {
		return jpaMethodRepository.findByMethodIdAndTenant_TenantId(id, tenantId)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.MEASUREMENT_METHOD_NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<MeasurementMethod> findAll(Long tenantId) {
		return mapper.toDomainList(jpaMethodRepository.findAllByTenant_TenantIdOrderBySortOrderAscMethodIdAsc(tenantId));
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByNameAndTenantId(String name, Long tenantId) {
		return jpaMethodRepository.existsByNameAndTenant_TenantId(name, tenantId);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByNameAndTenantIdAndIdNot(String name, Long tenantId, Long id) {
		return jpaMethodRepository.existsByNameAndTenant_TenantIdAndMethodIdNot(name, tenantId, id);
	}

	@Override
	@Transactional(readOnly = true)
	public Integer findMaxSortOrder(Long tenantId) {
		return jpaMethodRepository.findMaxSortOrder(tenantId);
	}

	@Override
	public void deleteById(Long id, Long tenantId) {
		int deletedCount = jpaMethodRepository.deleteByMethodIdAndTenantId(id, tenantId);

		if (deletedCount == 0) {
			throw new CustomException(ErrorCode.MEASUREMENT_METHOD_NOT_FOUND);
		}
	}
}
