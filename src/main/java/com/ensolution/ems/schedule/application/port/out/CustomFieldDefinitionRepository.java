package com.ensolution.ems.schedule.application.port.out;

import com.ensolution.ems.schedule.domain.custom_field.CustomFieldDefinition;

import java.util.List;

public interface CustomFieldDefinitionRepository {
	CustomFieldDefinition save(CustomFieldDefinition definition);

	/** 없거나 타 tenant면 Adapter에서 {@code CUSTOM_FIELD_NOT_FOUND}. */
	CustomFieldDefinition findById(Long id, Long tenantId);

	/**
	 * {@code sortOrder} 오름차순(tie-breaker는 PK). 회차 값 저장의 키 검증과 템플릿 검사의
	 * "알려진 커스텀 키"가 모두 이 목록에서 나온다.
	 */
	List<CustomFieldDefinition> findAll(Long tenantId);

	/** 부모 aggregate가 없는 전역 유일 체크이므로 tenantId를 함께 받는다. */
	boolean existsByKeyAndTenantId(String key, Long tenantId);

	/** 등록 시 맨 뒤에 붙이기 위한 현재 최댓값. 없으면 0. */
	Integer findMaxSortOrder(Long tenantId);

	/** 원자 삭제. 0건이면 {@code CUSTOM_FIELD_NOT_FOUND}. 스냅샷 문서에 남은 값은 건드리지 않는다. */
	void deleteById(Long id, Long tenantId);
}
