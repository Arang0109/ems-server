package com.ensolution.ems.tenant.infrastructure.adapter;

import com.ensolution.ems.tenant.application.command.list_item.StackPollutantListItem;
import com.ensolution.ems.tenant.domain.StackPollutant;
import com.ensolution.ems.tenant.application.port.out.StackPollutantRepository;
import com.ensolution.ems.tenant.infrastructure.entity.StackPollutantEntity;
import com.ensolution.ems.tenant.infrastructure.mapper.StackPollutantEntityMapper;
import com.ensolution.ems.tenant.infrastructure.repository.PollutantJpaRepository;
import com.ensolution.ems.tenant.infrastructure.repository.StackPollutantJpaRepository;
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
public class StackPollutantRepositoryAdapter implements StackPollutantRepository {

	private final StackPollutantJpaRepository jpaStackPollutantRepository;
	private final TenantJpaRepository jpaTenantRepository;
	private final StackJpaRepository jpaStackRepository;
	private final PollutantJpaRepository jpaPollutantRepository;
	private final StackPollutantEntityMapper mapper;

	@Override
	public StackPollutant save(StackPollutant stackPollutant) {
		StackPollutantEntity entity = mapper.toEntity(stackPollutant)
			.toBuilder()
			.tenant(jpaTenantRepository.getReferenceById(stackPollutant.getTenantId()))
			.stack(jpaStackRepository.getReferenceById(stackPollutant.getStackId()))
			.pollutant(jpaPollutantRepository.getReferenceById(stackPollutant.getPollutantId()))
			.build();
		return mapper.toDomain(jpaStackPollutantRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public List<StackPollutantListItem> findByStackId(Long stackId, Long tenantId) {
		return mapper.toListItems(jpaStackPollutantRepository.findByStackId(stackId, tenantId));
	}

	@Override
	public void deleteById(Long id, Long tenantId) {
		int deletedCount = jpaStackPollutantRepository.deleteByStackPollutantIdAndTenantId(id, tenantId);

		if (deletedCount == 0) {
			throw new CustomException(ErrorCode.NOT_FOUND);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByStackIdAndPollutantId(Long stackId, Long pollutantId) {
		return jpaStackPollutantRepository.existsByStack_StackIdAndPollutant_PollutantId(stackId, pollutantId);
	}
}
