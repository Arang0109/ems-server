package com.ensolution.ems.client_management.application.validator;

import com.ensolution.ems.client_management.application.port.out.MeasurementMethodRepository;
import com.ensolution.ems.client_management.application.port.out.PollutantRepository;
import com.ensolution.ems.client_management.domain.MeasurementMethod;
import com.ensolution.ems.client_management.domain.PollutantCatalog;
import com.ensolution.ems.global.common.enums.SampleGrouping;
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
	private final MeasurementMethodRepository measurementMethodRepository;

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

	/**
	 * 지정한 측정방법이 이 고객사의 것인지 확인한다. 다른 애그리거트를 참조하는 검증이라 Validator가 맡는다
	 * ({@code StackPollutantValidator.requirePollutantOwned}와 같은 자리).
	 * 미존재·타 tenant 모두 {@code MEASUREMENT_METHOD_NOT_FOUND}로 은닉한다.
	 *
	 * <p>수정 경로는 측정방법을 보내지 않으면 기존 값을 유지하므로 null이면 검사할 것이 없다.
	 */
	public void requireMethodOwned(Long methodId, Long tenantId) {
		if (methodId == null) return;

		measurementMethodRepository.findById(methodId, tenantId);
	}

	/**
	 * 항목별 채취시간 오버라이드는 항목마다 따로 잡는 방법에서만 뜻이 있다. 한 병으로 함께 잡는(MERGED) 방법의
	 * 항목에 시간을 따로 두면 "한 병인데 항목마다 시간이 다르다"는 모순이라 거부한다.
	 *
	 * <p>{@code methodId}는 이 수정 뒤 실제로 적용될 측정방법이다(수정 경로는 호출자가 기존 값과 병합해 넘긴다).
	 * 측정방법이 정해지지 않은 레거시 행(null)은 오버라이드만이 유일한 값이므로 허용한다.
	 */
	public void requireSamplingMinutesAllowed(Long methodId, Integer samplingMinutes, Long tenantId) {
		if (samplingMinutes == null || methodId == null) return;

		MeasurementMethod method = measurementMethodRepository.findById(methodId, tenantId);
		if (method.getSampleGrouping() == SampleGrouping.MERGED) {
			throw new CustomException(ErrorCode.POLLUTANT_SAMPLING_MINUTES_NOT_ALLOWED);
		}
	}
}
