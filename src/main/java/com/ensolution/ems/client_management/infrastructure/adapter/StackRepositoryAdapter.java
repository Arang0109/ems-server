package com.ensolution.ems.client_management.infrastructure.adapter;

import com.ensolution.ems.client_management.application.command.list_item.StackListItem;
import com.ensolution.ems.client_management.domain.Stack;
import com.ensolution.ems.client_management.domain.port.StackRepository;
import com.ensolution.ems.client_management.infrastructure.entity.StackEntity;
import com.ensolution.ems.client_management.infrastructure.repository.StackJpaRepository;
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
	private final StackEntityMapper mapper;

	@Override
	public Stack save(Stack stack) {
			StackEntity entity = mapper.toEntity(stack)
					.toBuilder()
					.workplace(jpaWorkplaceRepository.getReferenceById(stack.getWorkplaceId()))
					.build();
			return mapper.toDomain(jpaStackRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public Stack findById(Long id) {
			return jpaStackRepository.findById(id)
					.map(mapper::toDomain)
					.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
	}
	
	@Override
	public List<StackListItem> findAll() {
		return mapper.toStackListItems(jpaStackRepository.findAll());
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<StackListItem> findByWorkplaceId(Long workplaceId) {
			return mapper.toStackListItems(jpaStackRepository.findByWorkplaceId(workplaceId));
	}

	@Override
	public void deleteById(Long id) {
			jpaStackRepository.deleteById(id);
	}
	
	@Override
	@Transactional(readOnly = true)
	public Map<Long, List<MeasurementField>> findFieldsByWorkplaceIds(List<Long> workplaceIds) {
		if (workplaceIds.isEmpty()) return Map.of();
		return jpaStackRepository.findByWorkplaceIds(workplaceIds).stream()
			.collect(Collectors.groupingBy(
				e -> e.getWorkplace().getId(),
				Collectors.mapping(StackEntity::getField,
					Collectors.collectingAndThen(Collectors.toList(),
						list -> list.stream().distinct().toList()))
			));
	}

	@Override
	public boolean existsByNameAndWorkplaceIdAndField(String name, Long workplaceId, MeasurementField field) {
		return jpaStackRepository.existsByNameAndWorkplaceIdAndField(name, workplaceId, field);
	}
}
