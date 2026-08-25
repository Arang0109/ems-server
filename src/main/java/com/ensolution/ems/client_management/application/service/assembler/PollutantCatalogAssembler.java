package com.ensolution.ems.client_management.application.service.assembler;

import com.ensolution.ems.client_management.application.port.out.PollutantCatalogRepository;
import com.ensolution.ems.client_management.application.port.out.PollutantRepository;
import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.client_management.domain.PollutantCatalog;
import com.ensolution.ems.global.common.enums.MeasurementField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 지원 물질 가이드(카탈로그)와 이 tenant의 채택 현황을 대조한다.
 *
 * <p>값을 병합하지는 않는다 — 고객사 표기값은 {@code pollutants}가 소유하고, 카탈로그 속성
 * ({@code code}·{@code field}·{@code method}·{@code phase})은 {@code PollutantEntityMapper}가
 * 조인으로 이미 채워 둔다.
 *
 * <p>조회는 소스당 1회로 끝낸다. 항목 수에 비례해 쿼리가 늘지 않도록 in-memory 조인한다.
 */
@Component
@RequiredArgsConstructor
public class PollutantCatalogAssembler {

	private final PollutantRepository pollutantRepository;
	private final PollutantCatalogRepository pollutantCatalogRepository;
	
	public List<PollutantCatalog> assembleCandidates(Long tenantId, MeasurementField field) {
		Set<Long> adopted = findTenantPollutants(tenantId, field).stream()
			.map(Pollutant::getCatalogId)
			.collect(Collectors.toSet());

		return pollutantCatalogRepository.findAllActive(field).stream()
			.filter(catalog -> !adopted.contains(catalog.getId()))
			.toList();
	}

	public Map<Long, Pollutant> pollutantById(Long tenantId) {
		return pollutantRepository.findAll(tenantId).stream()
			.collect(Collectors.toMap(Pollutant::getId, Function.identity(), (a, b) -> a));
	}

	private List<Pollutant> findTenantPollutants(Long tenantId, MeasurementField field) {
		return field == null
			? pollutantRepository.findAll(tenantId)
			: pollutantRepository.findByField(field, tenantId);
	}
}
