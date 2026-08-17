package com.ensolution.ems.client_management.application.service.assembler;

import com.ensolution.ems.client_management.application.command.list_item.PollutantListItem;
import com.ensolution.ems.client_management.application.port.out.PollutantCatalogRepository;
import com.ensolution.ems.client_management.application.port.out.PollutantRepository;
import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.client_management.domain.PollutantCatalog;
import com.ensolution.ems.client_management.domain.PollutantSource;
import com.ensolution.ems.global.common.enums.MeasurementField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 지원 물질 가이드(카탈로그)와 이 tenant의 채택 현황을 합쳐 <b>선택 목록</b>을 만든다.
 *
 * <p>값 자체를 병합하지는 않는다 — 고객사 표기값은 {@code pollutants}가 소유하고, 카탈로그 속성
 * ({@code code}·{@code field}·{@code method}·{@code phase})은 {@code PollutantEntityMapper}가
 * 조인으로 이미 채워 둔다. 여기서는 "아직 채택하지 않은 항목"을 카탈로그에서 끌어와 합치는 조립만 한다.
 *
 * <p>조회는 소스별 1회씩, 총 2회로 끝낸다. 항목 수에 비례해 쿼리가 늘지 않도록 in-memory 조인한다.
 */
@Component
@RequiredArgsConstructor
public class PollutantCatalogAssembler {

	private final PollutantRepository pollutantRepository;
	private final PollutantCatalogRepository pollutantCatalogRepository;

	/**
	 * tenant가 선택할 수 있는 측정물질 전체.
	 * 폐지된 가이드 항목은 목록에서 빼되, 이미 채택해 쓰고 있는 tenant에게는 계속 보여 준다.
	 *
	 * <p>폐지 항목까지 한 번에 읽는 이유는 항목별 재조회를 없애기 위해서다.
	 * {@code findAllActive}로 좁히면 "폐지됐지만 이미 쓰는 항목"을 건건이 다시 읽어야 한다.
	 */
	public List<PollutantListItem> assembleSelectable(Long tenantId, MeasurementField field) {
		List<PollutantCatalog> catalogs = pollutantCatalogRepository.findAll(field);
		Map<Long, Pollutant> byCatalogId = findTenantPollutants(tenantId, field).stream()
			.collect(Collectors.toMap(Pollutant::getCatalogId, Function.identity(), (a, b) -> a));

		List<PollutantListItem> items = new ArrayList<>();
		for (PollutantCatalog catalog : catalogs) {
			Pollutant adopted = byCatalogId.get(catalog.getId());
			if (adopted == null) {
				if (!catalog.isActive()) continue;
				items.add(toCatalogItem(catalog));
				continue;
			}
			items.add(toItem(adopted, catalog.getSortOrder()));
		}
		return items;
	}

	/**
	 * 이 tenant의 측정물질을 id로 찾을 수 있게 돌려준다.
	 * 시설별 측정물질 목록·측정계획 스냅샷처럼 pollutantId로 조회하는 경로가 사용한다.
	 *
	 * <p>카탈로그 속성은 어댑터가 조인으로 이미 채워 두므로 여기서는 카탈로그를 읽지 않는다(조회 1회).
	 */
	public Map<Long, Pollutant> pollutantById(Long tenantId) {
		return pollutantRepository.findAll(tenantId).stream()
			.collect(Collectors.toMap(Pollutant::getId, Function.identity(), (a, b) -> a));
	}

	private List<Pollutant> findTenantPollutants(Long tenantId, MeasurementField field) {
		return field == null
			? pollutantRepository.findAll(tenantId)
			: pollutantRepository.findByField(field, tenantId);
	}

	/** 아직 채택하지 않은 가이드 항목. 고객사 소유값(영문명·장비·시험방법)은 아직 없다. */
	private PollutantListItem toCatalogItem(PollutantCatalog catalog) {
		return new PollutantListItem(
			null,
			catalog.getId(),
			catalog.getCode(),
			PollutantSource.CATALOG,
			catalog.getField(),
			catalog.getNameKr(),
			null,
			catalog.getMethod(),
			catalog.getPhase(),
			null,
			null,
			catalog.getSortOrder()
		);
	}

	private PollutantListItem toItem(Pollutant pollutant, Integer sortOrder) {
		return new PollutantListItem(
			pollutant.getId(),
			pollutant.getCatalogId(),
			pollutant.getCode(),
			PollutantSource.REGISTERED,
			pollutant.getField(),
			pollutant.getNameKr(),
			pollutant.getNameEn(),
			pollutant.getMethod(),
			pollutant.getPhase(),
			pollutant.getEquipment(),
			pollutant.getTestMethod(),
			sortOrder
		);
	}
}
