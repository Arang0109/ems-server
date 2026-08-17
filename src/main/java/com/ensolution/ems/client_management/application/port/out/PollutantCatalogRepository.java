package com.ensolution.ems.client_management.application.port.out;

import com.ensolution.ems.client_management.domain.PollutantCatalog;
import com.ensolution.ems.global.common.enums.MeasurementField;

import java.util.List;

/**
 * 전역 측정물질 카탈로그 아웃바운드 포트.
 * 전 tenant 공유 마스터이므로 다른 포트와 달리 {@code tenantId} 파라미터를 받지 않는다.
 */
public interface PollutantCatalogRepository {
	PollutantCatalog save(PollutantCatalog catalog);

	/** PK 단건 조회. 없으면 Adapter에서 {@code POLLUTANT_CATALOG_NOT_FOUND}. */
	PollutantCatalog findById(Long id);

	/**
	 * 측정분야 + code 단건 조회. 없으면 Adapter에서 {@code POLLUTANT_CATALOG_NOT_FOUND}.
	 * code는 분야 안에서만 유일하다 — 대기 납과 수질 납이 모두 {@code PB}이므로 분야 없이는 특정할 수 없다.
	 */
	PollutantCatalog findByFieldAndCode(MeasurementField field, String code);

	/** 폐지 항목 포함 전체. 운영자 화면용. */
	List<PollutantCatalog> findAll(MeasurementField field);

	/** 사용 중(active)인 항목만. tenant 선택 목록용. */
	List<PollutantCatalog> findAllActive(MeasurementField field);

	boolean existsByFieldAndCode(MeasurementField field, String code);
}
