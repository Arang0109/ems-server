package com.ensolution.ems.client_management.infrastructure.adapter;

import com.ensolution.ems.client_management.application.command.WorkplaceListItem;
import com.ensolution.ems.client_management.domain.Workplace;
import com.ensolution.ems.client_management.domain.port.WorkplaceRepository;
import com.ensolution.ems.client_management.infrastructure.repository.JpaCompanyRepository;
import com.ensolution.ems.client_management.infrastructure.entity.JpaWorkplaceEntity;
import com.ensolution.ems.client_management.infrastructure.repository.JpaWorkplaceRepository;
import com.ensolution.ems.client_management.infrastructure.mapper.WorkplaceDomainEntityMapper;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional
public class WorkplaceRepositoryAdapter implements WorkplaceRepository {

	private final JpaWorkplaceRepository jpaWorkplaceRepository;
	private final JpaCompanyRepository jpaCompanyRepository;
	private final WorkplaceDomainEntityMapper mapper;

	@Override
	public Workplace save(Workplace workplace) {
		if (workplace.getId() != null) {
			JpaWorkplaceEntity existing = jpaWorkplaceRepository.findById(workplace.getId())
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
			JpaWorkplaceEntity updated = mapper.toEntity(workplace).toBuilder()
				.company(existing.getCompany())
				.stacks(existing.getStacks())
				.build();
			return mapper.toDomain(jpaWorkplaceRepository.save(updated));
		}
		JpaWorkplaceEntity entity = mapper.toEntity(workplace)
			.toBuilder()
			.company(jpaCompanyRepository.getReferenceById(workplace.getCompanyId()))
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
	public List<WorkplaceListItem> findByCompanyId(Long companyId) {
		return mapper.toWorkplaceListItems(jpaWorkplaceRepository.findByCompanyId(companyId));
	}

	@Override
	public void deleteById(Long id) {
			jpaWorkplaceRepository.deleteById(id);
	}
	
	@Override
	public boolean existsByNameAndCompanyId(String name, Long companyId) {
		return jpaWorkplaceRepository.existsByNameAndCompanyId(name, companyId);
	}
}
