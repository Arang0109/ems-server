package com.ensolution.ems.platform.infrastructure.adapter;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.platform.application.command.TenantListItem;
import com.ensolution.ems.platform.application.port.out.TenantRepository;
import com.ensolution.ems.platform.domain.Tenant;
import com.ensolution.ems.platform.infrastructure.mapper.TenantEntityMapper;
import com.ensolution.ems.platform.infrastructure.repository.TenantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * TenantRepository 구현체. JPA 영속 앵커인 tenant 모듈의 TenantJpaRepository(멀티테넌시 공용)를 재사용한다.
 * (global 보안 코드가 동일 JpaRepository를 재사용하는 선례와 동일한 패턴)
 */
@Repository
@RequiredArgsConstructor
@Transactional
public class TenantRepositoryAdapter implements TenantRepository {

	private final TenantJpaRepository jpaTenantRepository;
	private final TenantEntityMapper mapper;

	@Override
	public Tenant save(Tenant tenant) {
		return mapper.toDomain(jpaTenantRepository.save(mapper.toEntity(tenant)));
	}

	@Override
	@Transactional(readOnly = true)
	public Tenant findById(Long id) {
		return jpaTenantRepository.findByTenantId(id)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.TENANT_NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<TenantListItem> findAll() {
		return mapper.toListItems(jpaTenantRepository.findAll());
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByBizNumber(String bizNumber) {
		return jpaTenantRepository.existsByBizNumber(bizNumber);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Tenant> findByBizNumber(String bizNumber) {
		return jpaTenantRepository.findByBizNumber(bizNumber).map(mapper::toDomain);
	}
}
