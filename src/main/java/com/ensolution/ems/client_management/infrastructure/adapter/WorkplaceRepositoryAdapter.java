package com.ensolution.ems.client_management.infrastructure.adapter;

import com.ensolution.ems.client_management.application.command.list_item.WorkplaceListItem;
import com.ensolution.ems.client_management.domain.Workplace;
import com.ensolution.ems.client_management.application.port.out.WorkplaceRepository;
import com.ensolution.ems.client_management.infrastructure.repository.ClientJpaRepository;
import com.ensolution.ems.client_management.infrastructure.entity.WorkplaceEntity;
import com.ensolution.ems.client_management.infrastructure.repository.WorkplaceJpaRepository;
import com.ensolution.ems.client_management.infrastructure.mapper.WorkplaceEntityMapper;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional
public class WorkplaceRepositoryAdapter implements WorkplaceRepository {

	private final WorkplaceJpaRepository jpaWorkplaceRepository;
	private final ClientJpaRepository jpaClientRepository;
	private final WorkplaceEntityMapper mapper;

	@Override
	public Workplace save(Workplace workplace) {
		if (workplace.getId() != null) {
			WorkplaceEntity existing = jpaWorkplaceRepository.findById(workplace.getId())
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
			WorkplaceEntity updated = mapper.toEntity(workplace).toBuilder()
				.client(existing.getClient())
				.stacks(existing.getStacks())
				.build();
			return mapper.toDomain(jpaWorkplaceRepository.save(updated));
		}
		WorkplaceEntity entity = mapper.toEntity(workplace)
			.toBuilder()
			.client(jpaClientRepository.getReferenceById(workplace.getClientId()))
			.build();
		return mapper.toDomain(jpaWorkplaceRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public Workplace findById(Long id) {
		return jpaWorkplaceRepository.findById(id)
			.map(mapper::toDomain).orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
	}

	@Override
	public List<WorkplaceListItem> findAll() {
		return mapper.toWorkplaceListItems(jpaWorkplaceRepository.findAll());
	}

	@Override
	@Transactional(readOnly = true)
	public List<WorkplaceListItem> findByClientId(Long clientId) {
		return mapper.toWorkplaceListItems(jpaWorkplaceRepository.findByClientId(clientId));
	}

	@Override
	public void deleteById(Long id) {
			jpaWorkplaceRepository.deleteById(id);
	}

	@Override
	public boolean existsById(Long workplaceId) { return jpaWorkplaceRepository.existsById(workplaceId); }

	@Override
	public boolean existsByNameAndClientId(String name, Long clientId) {
		return jpaWorkplaceRepository.existsByNameAndClientId(name, clientId);
	}
}
