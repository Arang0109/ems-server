package com.ensolution.ems.client_management.application.validator;

import com.ensolution.ems.client_management.application.port.out.MeasurementMethodRepository;
import com.ensolution.ems.client_management.application.port.out.PollutantRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 측정방법(MeasurementMethod) 생성·수정·삭제 시의 비즈니스 규칙 검증을 담당한다.
 * 단건 존재·소유권 검증은 Adapter의 {@code findById(id, tenantId)}가, 통칭명↔채취 단위 불변식은
 * 도메인({@code MeasurementMethod#requireConsistentGrouping})이 담당하므로 여기서 다루지 않는다.
 */
@Component
@RequiredArgsConstructor
public class MeasurementMethodValidator {

	private final MeasurementMethodRepository measurementMethodRepository;
	private final PollutantRepository pollutantRepository;

	/**
	 * 측정방법명은 tenant 안에서 유일해야 한다.
	 * 부모 aggregate가 없는 전역 유일 체크이므로 tenantId를 함께 받는다.
	 */
	public void requireUniqueName(String name, Long tenantId) {
		if (measurementMethodRepository.existsByNameAndTenantId(name, tenantId)) {
			throw new CustomException(ErrorCode.MEASUREMENT_METHOD_ALREADY_EXISTS);
		}
	}

	/**
	 * 수정용 — 자기 자신은 제외한다. 이름을 보내지 않은(유지) 요청은 검사할 것이 없다.
	 */
	public void requireUniqueNameExcluding(String name, Long tenantId, Long methodId) {
		if (name == null || name.isBlank()) return;

		if (measurementMethodRepository.existsByNameAndTenantIdAndIdNot(name, tenantId, methodId)) {
			throw new CustomException(ErrorCode.MEASUREMENT_METHOD_ALREADY_EXISTS);
		}
	}

	/**
	 * 측정물질이 참조 중이면 삭제를 막는다. 스냅샷은 사본이라 영향이 없지만 원장의 FK가 끊긴다.
	 *
	 * <p>{@code methodId}만으로 조회하므로 <b>호출 전에 {@code findById(id, tenantId)}로 소유권을 먼저
	 * 확인해야 한다.</b> 순서가 바뀌면 타 tenant의 id가 404 대신 409를 받아 존재가 드러난다.
	 */
	public void requireNotReferenced(Long methodId) {
		if (pollutantRepository.existsByMethodId(methodId)) {
			throw new CustomException(ErrorCode.MEASUREMENT_METHOD_IN_USE);
		}
	}
}
