package com.ensolution.ems.client_management.infrastructure.adapter;

import com.ensolution.ems.client_management.application.command.list_item.StackListItem;
import com.ensolution.ems.client_management.domain.Stack;
import com.ensolution.ems.client_management.application.port.out.StackRepository;
import com.ensolution.ems.client_management.infrastructure.entity.StackEntity;
import com.ensolution.ems.client_management.infrastructure.repository.StackJpaRepository;
import com.ensolution.ems.platform.infrastructure.repository.TenantJpaRepository;
import com.ensolution.ems.client_management.infrastructure.repository.WorkplaceJpaRepository;
import com.ensolution.ems.client_management.infrastructure.mapper.StackEntityMapper;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Transactional
public class StackRepositoryAdapter implements StackRepository {

	private final StackJpaRepository jpaStackRepository;
	private final WorkplaceJpaRepository jpaWorkplaceRepository;
	private final TenantJpaRepository jpaTenantRepository;
	private final StackEntityMapper mapper;

	@Override
	public Stack save(Stack stack) {
		if (stack.getId() != null) {
			StackEntity existing = jpaStackRepository.findByStackIdAndTenant_TenantId(stack.getId(), stack.getTenantId())
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
			StackEntity update = mapper.toEntity(stack).toBuilder()
				.tenant(jpaTenantRepository.getReferenceById(stack.getTenantId()))
				.workplace(existing.getWorkplace())
				.build();
			return mapper.toDomain(jpaStackRepository.save(update));
		}
		StackEntity entity = mapper.toEntity(stack)
			.toBuilder()
			.tenant(jpaTenantRepository.getReferenceById(stack.getTenantId()))
			.workplace(jpaWorkplaceRepository.getReferenceById(stack.getWorkplaceId()))
			.build();
		return mapper.toDomain(jpaStackRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public Stack findById(Long id, Long tenantId) {
		return jpaStackRepository.findByStackIdAndTenant_TenantId(id, tenantId)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
	}

	@Override
	public List<StackListItem> findAll(Long tenantId) {
		return mapper.toStackListItems(jpaStackRepository.findAllByTenant_TenantId(tenantId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<StackListItem> findByWorkplaceId(Long workplaceId, Long tenantId) {
		return mapper.toStackListItems(jpaStackRepository.findByWorkplace_WorkplaceIdAndTenant_TenantId(workplaceId, tenantId));
	}

	@Override
	public void deleteById(Long id, Long tenantId) {
		int deletedCount = jpaStackRepository.deleteByStackIdAndTenantId(id, tenantId);
		if (deletedCount == 0) {
			throw new CustomException(ErrorCode.NOT_FOUND);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Map<Long, List<MeasurementField>> findFieldsByWorkplaceIds(List<Long> workplaceIds, Long tenantId) {
		if (workplaceIds.isEmpty()) return Map.of();
		return jpaStackRepository.findByWorkplaceIds(workplaceIds, tenantId).stream()
			.collect(Collectors.groupingBy(
				e -> e.getWorkplace().getWorkplaceId(),
				Collectors.mapping(StackEntity::getField,
					Collectors.collectingAndThen(Collectors.toList(),
						list -> list.stream().distinct().toList()))
			));
	}

	@Override
	public boolean existsByNameAndWorkplaceIdAndField(String name, Long workplaceId, MeasurementField field) {
		return jpaStackRepository.existsByNameAndWorkplace_WorkplaceIdAndField(name, workplaceId, field);
	}
}
