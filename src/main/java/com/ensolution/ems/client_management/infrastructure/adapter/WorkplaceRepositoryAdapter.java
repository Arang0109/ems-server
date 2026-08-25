package com.ensolution.ems.client_management.infrastructure.adapter;

import com.ensolution.ems.client_management.application.command.list_item.WorkplaceListItem;
import com.ensolution.ems.client_management.domain.Workplace;
import com.ensolution.ems.client_management.application.port.out.WorkplaceRepository;
import com.ensolution.ems.client_management.infrastructure.repository.ClientJpaRepository;
import com.ensolution.ems.platform.infrastructure.repository.TenantJpaRepository;
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
	private final TenantJpaRepository jpaTenantRepository;
	private final WorkplaceEntityMapper mapper;

	@Override
	public Workplace save(Workplace workplace) {
		if (workplace.getId() != null) {
			jpaWorkplaceRepository.findByWorkplaceIdAndTenant_TenantId(workplace.getId(), workplace.getTenantId())
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
		}
		WorkplaceEntity entity = mapper.toEntity(workplace).toBuilder()
			.client(jpaClientRepository.getReferenceById(workplace.getClientId()))
			.tenant(jpaTenantRepository.getReferenceById(workplace.getTenantId()))
			.build();
		return mapper.toDomain(jpaWorkplaceRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public Workplace findById(Long id, Long tenantId) {
		return jpaWorkplaceRepository.findByWorkplaceIdAndTenant_TenantId(id, tenantId)
			.map(mapper::toDomain).orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
	}

	@Override
	public List<WorkplaceListItem> findAll(Long tenantId) {
		return mapper.toWorkplaceListItems(jpaWorkplaceRepository.findAllByTenant_TenantId(tenantId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<WorkplaceListItem> findByClientId(Long clientId, Long tenantId) {
		return mapper.toWorkplaceListItems(jpaWorkplaceRepository.findByClient_ClientIdAndTenant_TenantId(clientId, tenantId));
	}

	@Override
	public void deleteById(Long id, Long tenantId) {
		int deletedCount = jpaWorkplaceRepository.deleteByWorkplaceIdAndTenantId(id, tenantId);
		if (deletedCount == 0) {
			throw new CustomException(ErrorCode.NOT_FOUND);
		}
	}

	@Override
	public boolean existsById(Long workplaceId, Long tenantId) {
		return jpaWorkplaceRepository.existsByWorkplaceIdAndTenant_TenantId(workplaceId, tenantId);
	}

	@Override
	public boolean existsByNameAndClientId(String name, Long clientId) {
		return jpaWorkplaceRepository.existsByNameAndClient_ClientId(name, clientId);
	}
}
