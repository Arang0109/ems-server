package com.ensolution.ems.client_management.application;

import com.ensolution.ems.client_management.application.port.out.MeasurementMethodRepository;
import com.ensolution.ems.client_management.domain.MeasurementMethod;
import com.ensolution.ems.global.common.enums.SampleGrouping;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 인메모리 {@link MeasurementMethodRepository}. 실제 어댑터와 같은 예외를 던지고 tenant 필터를 재현해
 * 멀티테넌시 격리가 테스트로 검증되게 한다.
 */
public class FakeMeasurementMethodRepository implements MeasurementMethodRepository {

	private final List<MeasurementMethod> methods = new ArrayList<>();
	private long nextId = 1L;

	public MeasurementMethod given(Long tenantId, String name, SampleGrouping grouping,
	                               String mergedSampleName, Integer samplingMinutes) {
		return save(MeasurementMethod.register(tenantId, name, grouping, mergedSampleName, samplingMinutes, null, (int) nextId * 10));
	}

	@Override
	public MeasurementMethod save(MeasurementMethod method) {
		MeasurementMethod saved = method.getId() == null
			? method.toBuilder().id(nextId++).build()
			: method;
		methods.removeIf(m -> Objects.equals(m.getId(), saved.getId()));
		methods.add(saved);
		return saved;
	}

	@Override
	public MeasurementMethod findById(Long id, Long tenantId) {
		return methods.stream()
			.filter(m -> Objects.equals(m.getId(), id) && Objects.equals(m.getTenantId(), tenantId))
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorCode.MEASUREMENT_METHOD_NOT_FOUND));
	}

	@Override
	public List<MeasurementMethod> findAll(Long tenantId) {
		return methods.stream()
			.filter(m -> Objects.equals(m.getTenantId(), tenantId))
			.sorted(Comparator.comparing(MeasurementMethod::getSortOrder).thenComparing(MeasurementMethod::getId))
			.toList();
	}

	@Override
	public boolean existsByNameAndTenantId(String name, Long tenantId) {
		return methods.stream()
			.anyMatch(m -> Objects.equals(m.getTenantId(), tenantId) && Objects.equals(m.getName(), name));
	}

	@Override
	public boolean existsByNameAndTenantIdAndIdNot(String name, Long tenantId, Long id) {
		return methods.stream()
			.filter(m -> !Objects.equals(m.getId(), id))
			.anyMatch(m -> Objects.equals(m.getTenantId(), tenantId) && Objects.equals(m.getName(), name));
	}

	@Override
	public Integer findMaxSortOrder(Long tenantId) {
		return methods.stream()
			.filter(m -> Objects.equals(m.getTenantId(), tenantId))
			.map(MeasurementMethod::getSortOrder)
			.filter(Objects::nonNull)
			.max(Integer::compare)
			.orElse(0);
	}

	@Override
	public void deleteById(Long id, Long tenantId) {
		boolean removed = methods.removeIf(
			m -> Objects.equals(m.getId(), id) && Objects.equals(m.getTenantId(), tenantId));
		if (!removed) {
			throw new CustomException(ErrorCode.MEASUREMENT_METHOD_NOT_FOUND);
		}
	}
}
