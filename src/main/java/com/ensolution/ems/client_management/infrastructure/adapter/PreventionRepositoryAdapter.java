package com.ensolution.ems.client_management.infrastructure.adapter;

import com.ensolution.ems.client_management.application.port.out.PreventionRepository;
import com.ensolution.ems.client_management.domain.Prevention;
import com.ensolution.ems.client_management.infrastructure.entity.PreventionEntity;
import com.ensolution.ems.client_management.infrastructure.mapper.PreventionEntityMapper;
import com.ensolution.ems.client_management.infrastructure.repository.PreventionJpaRepository;
import com.ensolution.ems.client_management.infrastructure.repository.StackJpaRepository;
import com.ensolution.ems.platform.infrastructure.repository.TenantJpaRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Transactional
public class PreventionRepositoryAdapter implements PreventionRepository {

	private final PreventionJpaRepository jpaPreventionRepository;
	private final StackJpaRepository jpaStackRepository;
	private final TenantJpaRepository jpaTenantRepository;
	private final PreventionEntityMapper mapper;

	@Override
	public Prevention save(Prevention prevention) {
		if (prevention.getId() != null) {
			PreventionEntity existing = jpaPreventionRepository.findByPreventionIdAndTenant_TenantId(prevention.getId(), prevention.getTenantId())
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
			PreventionEntity update = mapper.toEntity(prevention).toBuilder()
				.tenant(existing.getTenant())
				.stack(existing.getStack())
				.build();
			return mapper.toDomain(jpaPreventionRepository.save(update));
		}
		PreventionEntity entity = mapper.toEntity(prevention)
			.toBuilder()
			.tenant(jpaTenantRepository.getReferenceById(prevention.getTenantId()))
			.stack(jpaStackRepository.getReferenceById(prevention.getStackId()))
			.build();
		return mapper.toDomain(jpaPreventionRepository.save(entity));
	}

	/**
	 * 순서 일괄 반영.
	 * <p>
	 * {@code save()} 를 건별로 반복하지 않는다 — 그 경로는 도메인으로부터 엔티티를 통째로 다시 만들어
	 * merge 하므로 조회도 N 번이고, 도메인이 모르는 컬럼이 생기면 곧바로 유실 경로가 된다.
	 * 여기서는 측정지점 단위로 한 번만 읽어 <b>기존 엔티티에서 sortOrder 만 갈아끼운다.</b>
	 */
	@Override
	public List<Prevention> saveAll(List<Prevention> preventions) {
		if (preventions.isEmpty()) {
			return List.of();
		}

		Prevention first = preventions.getFirst();
		Map<Long, PreventionEntity> byId = jpaPreventionRepository
			.findByStack_StackIdAndTenant_TenantIdOrderBySortOrderAscPreventionIdAsc(first.getStackId(), first.getTenantId())
			.stream()
			.collect(Collectors.toMap(PreventionEntity::getPreventionId, Function.identity()));

		List<PreventionEntity> updates = preventions.stream()
			.map(prevention -> {
				PreventionEntity existing = byId.get(prevention.getId());
				if (existing == null) {
					throw new CustomException(ErrorCode.NOT_FOUND);
				}
				return existing.toBuilder().sortOrder(prevention.getSortOrder()).build();
			})
			.toList();

		return mapper.toDomainList(jpaPreventionRepository.saveAll(updates));
	}

	@Override
	@Transactional(readOnly = true)
	public Integer findMaxSortOrder(Long stackId, Long tenantId) {
		return jpaPreventionRepository.findMaxSortOrder(stackId, tenantId);
	}

	@Override
	@Transactional(readOnly = true)
	public Prevention findById(Long id, Long tenantId) {
		return jpaPreventionRepository.findByPreventionIdAndTenant_TenantId(id, tenantId)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Prevention> findByStackId(Long stackId, Long tenantId) {
		return mapper.toDomainList(
			jpaPreventionRepository.findByStack_StackIdAndTenant_TenantIdOrderBySortOrderAscPreventionIdAsc(stackId, tenantId)
		);
	}

	@Override
	public void deleteById(Long id, Long tenantId) {
		int deletedCount = jpaPreventionRepository.deleteByPreventionIdAndTenantId(id, tenantId);

		if (deletedCount == 0) {
			throw new CustomException(ErrorCode.NOT_FOUND);
		}
	}
}
