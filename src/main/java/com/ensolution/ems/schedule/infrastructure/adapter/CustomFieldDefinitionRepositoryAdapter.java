package com.ensolution.ems.schedule.infrastructure.adapter;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.port.out.CustomFieldDefinitionRepository;
import com.ensolution.ems.schedule.domain.custom_field.CustomFieldDefinition;
import com.ensolution.ems.schedule.infrastructure.entity.CustomFieldDefinitionEntity;
import com.ensolution.ems.schedule.infrastructure.mapper.CustomFieldDefinitionEntityMapper;
import com.ensolution.ems.schedule.infrastructure.repository.CustomFieldDefinitionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional
public class CustomFieldDefinitionRepositoryAdapter implements CustomFieldDefinitionRepository {

	private final CustomFieldDefinitionJpaRepository jpaRepository;
	private final CustomFieldDefinitionEntityMapper mapper;

	@Override
	public CustomFieldDefinition save(CustomFieldDefinition definition) {
		CustomFieldDefinitionEntity entity = mapper.toEntity(definition);
		return mapper.toDomain(jpaRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public CustomFieldDefinition findById(Long id, Long tenantId) {
		return jpaRepository.findByFieldIdAndTenantId(id, tenantId)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.CUSTOM_FIELD_NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<CustomFieldDefinition> findAll(Long tenantId) {
		return mapper.toDomains(jpaRepository.findAllByTenantIdOrderBySortOrderAscFieldIdAsc(tenantId));
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByKeyAndTenantId(String key, Long tenantId) {
		return jpaRepository.existsByKeyAndTenantId(key, tenantId);
	}

	@Override
	@Transactional(readOnly = true)
	public Integer findMaxSortOrder(Long tenantId) {
		return jpaRepository.findMaxSortOrder(tenantId);
	}

	@Override
	public void deleteById(Long id, Long tenantId) {
		int deletedCount = jpaRepository.deleteByFieldIdAndTenantId(id, tenantId);

		if (deletedCount == 0) {
			throw new CustomException(ErrorCode.CUSTOM_FIELD_NOT_FOUND);
		}
	}
}
