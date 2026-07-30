package com.ensolution.ems.client_management.application.validator;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.client_management.application.port.out.PollutantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 측정물질(Pollutant) 생성·수정 시의 비즈니스 규칙 검증을 담당한다.
 * 단건 존재·소유권 검증은 Adapter의 {@code findById(id, tenantId)}가 담당하므로 여기서 다루지 않는다.
 */
@Component
@RequiredArgsConstructor
public class PollutantValidator {

	private final PollutantRepository pollutantRepository;

	/**
	 * 측정물질 국문명은 tenant 안에서 유일해야 한다.
	 * 부모 aggregate가 없는 전역 유일 체크이므로 tenantId를 함께 받는다.
	 */
	public void requireUniqueNameKr(String nameKr, Long tenantId) {
		if (pollutantRepository.existsByNameKrAndTenantId(nameKr, tenantId)) {
			throw new CustomException(ErrorCode.CONFLICT);
		}
	}
}
