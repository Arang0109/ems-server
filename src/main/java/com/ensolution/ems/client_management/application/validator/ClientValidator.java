package com.ensolution.ems.client_management.application.validator;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.client_management.application.port.out.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 의뢰기관(Client) 생성·수정 시의 비즈니스 규칙 검증을 담당한다.
 * 단건 존재·소유권 검증은 Adapter의 {@code findById(id, tenantId)}가 담당하므로 여기서 다루지 않는다.
 */
@Component
@RequiredArgsConstructor
public class ClientValidator {

	private final ClientRepository clientRepository;

	/** 의뢰기관명은 유일해야 한다. */
	public void requireUniqueName(String name) {
		if (clientRepository.existsByName(name)) {
			throw new CustomException(ErrorCode.CONFLICT);
		}
	}
}
