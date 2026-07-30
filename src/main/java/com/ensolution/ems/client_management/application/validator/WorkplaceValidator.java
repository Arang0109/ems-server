package com.ensolution.ems.client_management.application.validator;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.client_management.application.port.out.WorkplaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 사업장(Workplace) 생성·수정 시의 비즈니스 규칙 검증을 담당한다.
 * 단건 존재·소유권 검증은 Adapter의 {@code findById(id, tenantId)}가 담당하므로 여기서 다루지 않는다.
 */
@Component
@RequiredArgsConstructor
public class WorkplaceValidator {

	private final WorkplaceRepository workplaceRepository;

	/**
	 * 사업장명은 같은 의뢰기관 안에서 유일해야 한다.
	 * clientId가 이미 tenant에 종속되므로 tenant 파라미터를 받지 않는다.
	 */
	public void requireUniqueNameInClient(String name, Long clientId) {
		if (workplaceRepository.existsByNameAndClientId(name, clientId)) {
			throw new CustomException(ErrorCode.CONFLICT);
		}
	}
}
