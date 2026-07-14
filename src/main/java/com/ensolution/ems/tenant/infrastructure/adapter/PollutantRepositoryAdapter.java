package com.ensolution.ems.tenant.infrastructure.adapter;

import com.ensolution.ems.tenant.domain.Pollutant;
import com.ensolution.ems.tenant.application.port.out.PollutantRepository;
import com.ensolution.ems.tenant.infrastructure.entity.PollutantEntity;
import com.ensolution.ems.tenant.infrastructure.mapper.PollutantEntityMapper;
import com.ensolution.ems.tenant.infrastructure.repository.PollutantJpaRepository;
import com.ensolution.ems.tenant.infrastructure.repository.TenantJpaRepository;
import com.ensolution.ems.global.common.enums.MeasurementField;
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

	private final PollutantJpaRepository jpaPollutantRepository;
	private final TenantJpaRepository jpaTenantRepository;
	private final PollutantEntityMapper mapper;

	@Override
	public Pollutant save(Pollutant pollutant) {
		if (pollutant.getId() != null) {
			jpaPollutantRepository.findById(pollutant.getId())
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
		}
		PollutantEntity entity = mapper.toEntity(pollutant).toBuilder()
			.tenant(jpaTenantRepository.getReferenceById(pollutant.getTenantId()))
			.build();
		return mapper.toDomain(jpaPollutantRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public Pollutant findById(Long id, Long tenantId) {
		return jpaPollutantRepository.findByPollutantIdAndTenant_TenantId(id, tenantId)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Pollutant> findAll(Long tenantId) {
		return mapper.toDomainList(jpaPollutantRepository.findAllByTenant_TenantId(tenantId));
	}

	@Override
	public List<Pollutant> findByField(MeasurementField field, Long tenantId) {
		return mapper.toDomainList(jpaPollutantRepository.findByFieldAndTenant_TenantId(field, tenantId));
	}

	@Override
	public void deleteById(Long id, Long tenantId) {
		int deletedCount = jpaPollutantRepository.deleteByPollutantIdAndTenantId(id, tenantId);

		if (deletedCount == 0) {
			throw new CustomException(ErrorCode.NOT_FOUND);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByNameKrAndTenantId(String nameKr, Long tenantId) {
		return jpaPollutantRepository.existsByNameKrAndTenant_TenantId(nameKr, tenantId);
	}
}
