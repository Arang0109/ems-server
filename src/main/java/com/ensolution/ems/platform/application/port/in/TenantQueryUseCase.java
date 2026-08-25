package com.ensolution.ems.platform.application.port.in;

/**
 * 고객사(테넌트) 요약 정보를 타 모듈에 제공하는 인바운드 포트.
 * 미존재 tenantId는 TENANT_NOT_FOUND로 처리한다.
 */
public interface TenantQueryUseCase {
	TenantSummary getTenantSummary(Long tenantId);
}
