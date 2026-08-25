package com.ensolution.ems.client_management.infrastructure.adapter;

import com.ensolution.ems.client_management.application.port.out.FacilityRepository;
import com.ensolution.ems.client_management.domain.Facility;
import com.ensolution.ems.client_management.infrastructure.entity.FacilityEntity;
import com.ensolution.ems.client_management.infrastructure.mapper.FacilityEntityMapper;
import com.ensolution.ems.client_management.infrastructure.repository.FacilityJpaRepository;
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
public class FacilityRepositoryAdapter implements FacilityRepository {

	private final FacilityJpaRepository jpaFacilityRepository;
	private final StackJpaRepository jpaStackRepository;
	private final TenantJpaRepository jpaTenantRepository;
	private final FacilityEntityMapper mapper;

	@Override
	public Facility save(Facility facility) {
		if (facility.getId() != null) {
			FacilityEntity existing = jpaFacilityRepository.findByFacilityIdAndTenant_TenantId(facility.getId(), facility.getTenantId())
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
			FacilityEntity update = mapper.toEntity(facility).toBuilder()
				.tenant(existing.getTenant())
				.stack(existing.getStack())
				.build();
			return mapper.toDomain(jpaFacilityRepository.save(update));
		}
		FacilityEntity entity = mapper.toEntity(facility)
			.toBuilder()
			.tenant(jpaTenantRepository.getReferenceById(facility.getTenantId()))
			.stack(jpaStackRepository.getReferenceById(facility.getStackId()))
			.build();
		return mapper.toDomain(jpaFacilityRepository.save(entity));
	}

	/**
	 * 순서 일괄 반영.
	 * <p>
	 * {@code save()} 를 건별로 반복하지 않는다 — 그 경로는 도메인으로부터 엔티티를 통째로 다시 만들어
	 * merge 하므로 조회도 N 번이고, 도메인이 모르는 컬럼이 생기면 곧바로 유실 경로가 된다.
	 * 여기서는 측정지점 단위로 한 번만 읽어 <b>기존 엔티티에서 sortOrder 만 갈아끼운다.</b>
	 */
	@Override
	public List<Facility> saveAll(List<Facility> facilities) {
		if (facilities.isEmpty()) {
			return List.of();
		}

		Facility first = facilities.getFirst();
		Map<Long, FacilityEntity> byId = jpaFacilityRepository
			.findByStack_StackIdAndTenant_TenantIdOrderBySortOrderAscFacilityIdAsc(first.getStackId(), first.getTenantId())
			.stream()
			.collect(Collectors.toMap(FacilityEntity::getFacilityId, Function.identity()));

		List<FacilityEntity> updates = facilities.stream()
			.map(facility -> {
				FacilityEntity existing = byId.get(facility.getId());
				if (existing == null) {
					throw new CustomException(ErrorCode.NOT_FOUND);
				}
				return existing.toBuilder().sortOrder(facility.getSortOrder()).build();
			})
			.toList();

		return mapper.toDomainList(jpaFacilityRepository.saveAll(updates));
	}

	@Override
	@Transactional(readOnly = true)
	public Integer findMaxSortOrder(Long stackId, Long tenantId) {
		return jpaFacilityRepository.findMaxSortOrder(stackId, tenantId);
	}

	@Override
	@Transactional(readOnly = true)
	public Facility findById(Long id, Long tenantId) {
		return jpaFacilityRepository.findByFacilityIdAndTenant_TenantId(id, tenantId)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Facility> findByStackId(Long stackId, Long tenantId) {
		return mapper.toDomainList(
			jpaFacilityRepository.findByStack_StackIdAndTenant_TenantIdOrderBySortOrderAscFacilityIdAsc(stackId, tenantId)
		);
	}

	@Override
	public void deleteById(Long id, Long tenantId) {
		int deletedCount = jpaFacilityRepository.deleteByFacilityIdAndTenantId(id, tenantId);

		if (deletedCount == 0) {
			throw new CustomException(ErrorCode.NOT_FOUND);
		}
	}
}
