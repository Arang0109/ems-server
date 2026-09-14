package com.ensolution.ems.client_management.application.port.out;

import com.ensolution.ems.client_management.domain.MeasurementMethod;

import java.util.List;

public interface MeasurementMethodRepository {
	MeasurementMethod save(MeasurementMethod method);

	/** 없거나 타 tenant면 Adapter에서 {@code MEASUREMENT_METHOD_NOT_FOUND}. */
	MeasurementMethod findById(Long id, Long tenantId);

	/** {@code sortOrder} 오름차순(tie-breaker는 PK). 배열 순서가 곧 화면 표시 순서다. */
	List<MeasurementMethod> findAll(Long tenantId);

	boolean existsByNameAndTenantId(String name, Long tenantId);

	/** 수정 시 자기 자신을 제외한 이름 유일성 검사용. */
	boolean existsByNameAndTenantIdAndIdNot(String name, Long tenantId, Long id);

	/** 등록 시 맨 뒤에 붙이기 위한 현재 최댓값. 없으면 0. */
	Integer findMaxSortOrder(Long tenantId);

	void deleteById(Long id, Long tenantId);
}
