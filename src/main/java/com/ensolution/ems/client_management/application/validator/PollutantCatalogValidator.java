package com.ensolution.ems.client_management.application.validator;

import com.ensolution.ems.client_management.application.port.out.PollutantCatalogRepository;
import com.ensolution.ems.client_management.application.port.out.PollutantRepository;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 전역 측정물질 카탈로그의 비즈니스 규칙 검증. 전 tenant가 공유하는 마스터이므로 tenant 범위를 따지지 않는다.
 */
@Component
@RequiredArgsConstructor
public class PollutantCatalogValidator {

	private final PollutantCatalogRepository pollutantCatalogRepository;
	private final PollutantRepository pollutantRepository;

	/**
	 * code는 클라이언트 분기의 기준이므로 <b>측정분야 안에서</b> 유일해야 한다.
	 * 대기 납과 수질 납처럼 분야가 다르면 같은 code를 쓸 수 있다 — 기준·시험방법이 다른 별개 항목이기 때문이다.
	 */
	public void requireUniqueCode(MeasurementField field, String code) {
		if (pollutantCatalogRepository.existsByFieldAndCode(field, code)) {
			throw new CustomException(ErrorCode.POLLUTANT_CATALOG_CODE_DUPLICATED);
		}
	}

	/**
	 * 어느 tenant든 참조 중이면 삭제를 막는다.
	 * 폐지는 삭제가 아니라 {@code active=false}로 처리해야 기존 데이터·스냅샷이 온전히 남는다.
	 */
	public void requireNotReferenced(Long catalogId) {
		if (pollutantRepository.existsByCatalogId(catalogId)) {
			throw new CustomException(ErrorCode.POLLUTANT_CATALOG_IN_USE);
		}
	}
}
