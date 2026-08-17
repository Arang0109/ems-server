package com.ensolution.ems.client_management.application.port.out;

import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.global.common.enums.MeasurementField;

import java.util.List;

public interface PollutantRepository {
	Pollutant save(Pollutant pollutant);
	Pollutant findById(Long id, Long tenantId);
	List<Pollutant> findAll(Long tenantId);
	List<Pollutant> findByField(MeasurementField field, Long tenantId);

	/**
	 * 이 tenant가 해당 가이드 항목을 채택해 보유 중인 행. <b>없으면 null을 반환한다</b>(예외 아님) —
	 * 미채택은 "아직 만들지 않았다"는 정상 상태이며, 호출부가 행 생성 여부를 판단한다.
	 */
	Pollutant findByCatalogIdOrNull(Long catalogId, Long tenantId);

	/** 카탈로그 삭제 가능 여부 판단용(tenant 무관 전역 확인). */
	boolean existsByCatalogId(Long catalogId);

	void deleteById(Long id, Long tenantId);
}
