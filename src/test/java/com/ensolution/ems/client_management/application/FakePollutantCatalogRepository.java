package com.ensolution.ems.client_management.application;

import com.ensolution.ems.client_management.application.port.out.PollutantCatalogRepository;
import com.ensolution.ems.client_management.domain.PollutantCatalog;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 인메모리 {@link PollutantCatalogRepository}.
 * 조회 횟수를 세어 항목 수에 비례해 쿼리가 늘지 않는지(N+1) 검증할 수 있게 한다.
 */
public class FakePollutantCatalogRepository implements PollutantCatalogRepository {

	private final List<PollutantCatalog> catalogs = new ArrayList<>();
	private long nextId = 1L;

	private int listQueryCount = 0;
	private int findByIdCount = 0;

	public PollutantCatalog given(String code, MeasurementField field, String nameKr, Integer sortOrder) {
		return given(code, field, nameKr, sortOrder, true);
	}

	public PollutantCatalog given(String code, MeasurementField field, String nameKr, Integer sortOrder, boolean active) {
		return given(code, field, nameKr, sortOrder, active, null, null);
	}

	/** 카탈로그 투영값(method·phase)까지 지정한다. 투영이 실제로 전파되는지 검증할 때 쓴다. */
	public PollutantCatalog given(
		String code, MeasurementField field, String nameKr, Integer sortOrder, boolean active,
		MeasurementMethod method, PollutantPhase phase
	) {
		PollutantCatalog catalog = PollutantCatalog.builder()
			.id(nextId++)
			.code(code)
			.field(field)
			.nameKr(nameKr)
			.method(method)
			.phase(phase)
			.sortOrder(sortOrder)
			.active(active)
			.build();
		catalogs.add(catalog);
		return catalog;
	}

	public int listQueryCount() {
		return listQueryCount;
	}

	public int findByIdCount() {
		return findByIdCount;
	}

	@Override
	public PollutantCatalog findById(Long id) {
		findByIdCount++;
		return catalogs.stream()
			.filter(c -> Objects.equals(c.getId(), id))
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorCode.POLLUTANT_CATALOG_NOT_FOUND));
	}

	@Override
	public PollutantCatalog findByFieldAndCode(MeasurementField field, String code) {
		return catalogs.stream()
			.filter(c -> c.getField() == field && Objects.equals(c.getCode(), code))
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorCode.POLLUTANT_CATALOG_NOT_FOUND));
	}

	@Override
	public List<PollutantCatalog> findAll(MeasurementField field) {
		listQueryCount++;
		return filtered(field, false);
	}

	@Override
	public List<PollutantCatalog> findAllActive(MeasurementField field) {
		listQueryCount++;
		return filtered(field, true);
	}

	@Override
	public boolean existsByFieldAndCode(MeasurementField field, String code) {
		return catalogs.stream().anyMatch(c -> c.getField() == field && Objects.equals(c.getCode(), code));
	}

	@Override
	public PollutantCatalog save(PollutantCatalog catalog) {
		throw new UnsupportedOperationException();
	}

	private List<PollutantCatalog> filtered(MeasurementField field, boolean activeOnly) {
		return catalogs.stream()
			.filter(c -> !activeOnly || c.isActive())
			.filter(c -> field == null || field == c.getField())
			.sorted(Comparator.comparing(PollutantCatalog::getSortOrder,
				Comparator.nullsLast(Comparator.naturalOrder())))
			.toList();
	}
}
