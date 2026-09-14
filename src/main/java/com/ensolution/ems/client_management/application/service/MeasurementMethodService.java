package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.create.CreateMeasurementMethodCommand;
import com.ensolution.ems.client_management.application.command.update.UpdateMeasurementMethodCommand;
import com.ensolution.ems.client_management.application.port.out.MeasurementMethodRepository;
import com.ensolution.ems.client_management.application.validator.MeasurementMethodValidator;
import com.ensolution.ems.client_management.domain.MeasurementMethod;
import com.ensolution.ems.client_management.domain.MeasurementMethodPreset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 고객사 측정방법 유스케이스. 다른 모듈이 소비하지 않으므로 인바운드 포트를 두지 않는다 —
 * schedule은 측정방법을 {@code StackQueryUseCase}의 측정항목 요약에 실려 오는 사본으로만 본다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MeasurementMethodService {

	private static final int SORT_ORDER_STEP = 10;

	private final MeasurementMethodRepository measurementMethodRepository;
	private final MeasurementMethodValidator measurementMethodValidator;

	/** 등록. {@code sortOrder}를 주지 않으면 목록 맨 뒤({@code max+10})에 붙인다. */
	public MeasurementMethod createMeasurementMethod(CreateMeasurementMethodCommand command) {
		measurementMethodValidator.requireUniqueName(command.name(), command.tenantId());

		Integer sortOrder = command.sortOrder() != null
			? command.sortOrder()
			: measurementMethodRepository.findMaxSortOrder(command.tenantId()) + SORT_ORDER_STEP;

		return measurementMethodRepository.save(MeasurementMethod.register(
			command.tenantId(), command.name(), command.sampleGrouping(),
			command.mergedSampleName(), command.samplingMinutes(), sortOrder
		));
	}

	public MeasurementMethod updateMeasurementMethod(Long id, Long tenantId, UpdateMeasurementMethodCommand command) {
		MeasurementMethod method = measurementMethodRepository.findById(id, tenantId);
		measurementMethodValidator.requireUniqueNameExcluding(command.name(), tenantId, id);

		return measurementMethodRepository.save(method.update(
			command.name(), command.sampleGrouping(), command.mergedSampleName(), command.samplingMinutes()
		));
	}

	/**
	 * 삭제. 소유권 확인({@code findById})이 참조 확인보다 <b>먼저</b>다 — 순서가 바뀌면 타 tenant의 id에
	 * 409가 나가 존재가 드러난다.
	 */
	public void deleteMeasurementMethod(Long id, Long tenantId) {
		measurementMethodRepository.findById(id, tenantId);
		measurementMethodValidator.requireNotReferenced(id);
		measurementMethodRepository.deleteById(id, tenantId);
	}

	/**
	 * 기본 8종({@link MeasurementMethodPreset})을 <b>이름 기준으로 멱등하게</b> 채운다.
	 * 이미 같은 이름이 있으면 손대지 않는다 — 고객사가 API로 고친 값을 되돌리면 안 된다
	 * ({@code PollutantCatalogService.ensureCatalog}와 같은 방침). 이름을 바꾼 항목은 "없는 것"으로 보여
	 * 원래 이름이 다시 생기는데, 그것은 정책상 허용한다.
	 *
	 * <p>platform이 고객사를 발급할 때 자동으로 부르지 않는다. 모듈 간 참조가 생기기 때문이며, 고객사가
	 * 첫 화면에서 명시적으로 채운다.
	 */
	public List<MeasurementMethod> ensureDefaults(Long tenantId) {
		Set<String> existing = measurementMethodRepository.findAll(tenantId).stream()
			.map(MeasurementMethod::getName)
			.collect(Collectors.toSet());

		for (MeasurementMethodPreset preset : MeasurementMethodPreset.values()) {
			if (!existing.contains(preset.getMethodName())) {
				measurementMethodRepository.save(preset.toDomain(tenantId));
			}
		}
		return measurementMethodRepository.findAll(tenantId);
	}

	@Transactional(readOnly = true)
	public MeasurementMethod getMeasurementMethod(Long id, Long tenantId) {
		return measurementMethodRepository.findById(id, tenantId);
	}

	@Transactional(readOnly = true)
	public List<MeasurementMethod> getMeasurementMethodList(Long tenantId) {
		return measurementMethodRepository.findAll(tenantId);
	}
}
