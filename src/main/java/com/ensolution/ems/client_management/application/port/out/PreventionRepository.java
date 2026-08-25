package com.ensolution.ems.client_management.application.port.out;

import com.ensolution.ems.client_management.domain.Prevention;

import java.util.List;

public interface PreventionRepository {
	Prevention save(Prevention prevention);

	/** 순서 일괄 변경용. 한 트랜잭션에서 여러 건의 sortOrder 를 반영한다. */
	List<Prevention> saveAll(List<Prevention> preventions);

	Prevention findById(Long id, Long tenantId);

	/** sortOrder 오름차순으로 정렬해 돌려준다. 배열 순서가 곧 표시 순위다. */
	List<Prevention> findByStackId(Long stackId, Long tenantId);

	/** 신규 등록 시 목록 맨 뒤에 붙이기 위한 현재 최대 순서값. 시설이 없으면 0. */
	Integer findMaxSortOrder(Long stackId, Long tenantId);

	void deleteById(Long id, Long tenantId);
}
