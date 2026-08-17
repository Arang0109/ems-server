package com.ensolution.ems.client_management.application.validator;

import com.ensolution.ems.client_management.application.port.out.PollutantRepository;
import com.ensolution.ems.client_management.domain.PollutantCatalog;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 측정물질(Pollutant) 채택·수정 시의 비즈니스 규칙 검증을 담당한다.
 * 단건 존재·소유권 검증은 Adapter의 {@code findById(id, tenantId)}가 담당하므로 여기서 다루지 않는다.
 *
 * <p>{@code catalogId} 필수 여부는 Request DTO의 Bean Validation이, 존재 여부는
 * {@code PollutantCatalogRepository.findById()}가 담당한다.
 */
@Component
@RequiredArgsConstructor
public class PollutantValidator {

	private final PollutantRepository pollutantRepository;

	/**
	 * 폐지된 가이드 항목은 새로 채택할 수 없다.
	 * 이미 채택해 쓰고 있는 물질은 이 검증을 거치지 않으므로 계속 사용할 수 있다.
	 *
	 * <p>저장소를 다시 읽지 않도록 id가 아니라 이미 조회한 도메인 객체를 받는다.
	 */
	public void requireSelectable(PollutantCatalog catalog) {
		if (!catalog.isActive()) {
			throw new CustomException(ErrorCode.POLLUTANT_CATALOG_INACTIVE);
		}
	}

	/** 한 tenant는 같은 가이드 항목을 두 번 채택할 수 없다(물질당 행 하나). */
	public void requireCatalogNotLinked(Long catalogId, Long tenantId) {
		if (pollutantRepository.findByCatalogIdOrNull(catalogId, tenantId) != null) {
			throw new CustomException(ErrorCode.POLLUTANT_ALREADY_LINKED);
		}
	}
}
