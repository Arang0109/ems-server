package com.ensolution.ems.client_management.application;

import com.ensolution.ems.client_management.application.port.out.PollutantRepository;
import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.client_management.domain.PollutantCatalog;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 인메모리 {@link PollutantRepository}.
 * 조회 횟수를 세어 항목 수에 비례해 쿼리가 늘지 않는지(N+1) 검증할 수 있게 한다.
 */
public class FakePollutantRepository implements PollutantRepository {

	private final List<Pollutant> pollutants = new ArrayList<>();
	private long nextId = 100L;

	private int listQueryCount = 0;
	private int saveCount = 0;

	/**
	 * 이 tenant가 가이드 항목을 채택한 상태를 만든다.
	 * 어댑터가 조인으로 채우는 카탈로그 투영값(code·field·method·phase)까지 흉내낸다.
	 *
	 * @param nameKr 고객사 표기명. null이면 카탈로그 국문명을 복사한 상태가 된다
	 */
	public Pollutant given(Long tenantId, PollutantCatalog catalog, String nameKr) {
		Pollutant pollutant = Pollutant.register(tenantId, catalog, nameKr, null, null, null)
			.toBuilder()
			.id(nextId++)
			.build();
		pollutants.add(pollutant);
		return pollutant;
	}

	public int listQueryCount() {
		return listQueryCount;
	}

	public int saveCount() {
		return saveCount;
	}

	@Override
	public Pollutant save(Pollutant pollutant) {
		saveCount++;
		Pollutant saved = pollutant.getId() == null
			? pollutant.toBuilder().id(nextId++).build()
			: pollutant;
		pollutants.removeIf(p -> Objects.equals(p.getId(), saved.getId()));
		pollutants.add(saved);
		return saved;
	}

	@Override
	public Pollutant findById(Long id, Long tenantId) {
		return pollutants.stream()
			.filter(p -> Objects.equals(p.getId(), id) && Objects.equals(p.getTenantId(), tenantId))
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
	}

	@Override
	public List<Pollutant> findAll(Long tenantId) {
		listQueryCount++;
		return pollutants.stream()
			.filter(p -> Objects.equals(p.getTenantId(), tenantId))
			.toList();
	}

	@Override
	public List<Pollutant> findByField(MeasurementField field, Long tenantId) {
		listQueryCount++;
		return pollutants.stream()
			.filter(p -> Objects.equals(p.getTenantId(), tenantId))
			.filter(p -> field == null || field == p.getField())
			.toList();
	}

	@Override
	public Pollutant findByCatalogIdOrNull(Long catalogId, Long tenantId) {
		return pollutants.stream()
			.filter(p -> Objects.equals(p.getTenantId(), tenantId))
			.filter(p -> Objects.equals(p.getCatalogId(), catalogId))
			.findFirst()
			.orElse(null);
	}

	@Override
	public boolean existsByCatalogId(Long catalogId) {
		return pollutants.stream().anyMatch(p -> Objects.equals(p.getCatalogId(), catalogId));
	}

	@Override
	public void deleteById(Long id, Long tenantId) {
		throw new UnsupportedOperationException();
	}
}
