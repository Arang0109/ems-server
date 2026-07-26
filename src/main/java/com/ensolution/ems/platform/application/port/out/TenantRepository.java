package com.ensolution.ems.platform.application.port.out;

import com.ensolution.ems.platform.application.command.list_item.TenantListItem;
import com.ensolution.ems.platform.domain.Tenant;

import java.util.List;
import java.util.Optional;

/**
 * 고객사(테넌트) 아웃바운드 포트. 구현체는 tenant 모듈의 TenantJpaRepository(멀티테넌시 공용 앵커)를 재사용한다.
 */
public interface TenantRepository {
	Tenant save(Tenant tenant);

	/** PK 단건 조회. 없으면 Adapter에서 TENANT_NOT_FOUND. */
	Tenant findById(Long id);

	List<TenantListItem> findAll();

	boolean existsByBizNumber(String bizNumber);

	/** 시스템 테넌트 등 사업자번호 기반 조회(부트스트랩 멱등성 확인용). */
	Optional<Tenant> findByBizNumber(String bizNumber);
}
