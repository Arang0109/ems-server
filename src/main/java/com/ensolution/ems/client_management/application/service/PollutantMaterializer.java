package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.port.out.PollutantCatalogRepository;
import com.ensolution.ems.client_management.application.port.out.PollutantRepository;
import com.ensolution.ems.client_management.application.validator.PollutantValidator;
import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.client_management.domain.PollutantCatalog;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * 클라이언트가 가이드 항목을 고르면 이 tenant가 아직 채택하지 않았을 수 있다.
 * 시설별 측정물질은 {@code pollutants}를 참조하므로, 등록 시점에 행을 만들어 pollutantId를 확정한다.
 *
 * <p>Validator가 아니라 별도 컴포넌트인 이유: 쓰기 부작용이 있어 {@code require*()}(void/throw) 계약과 맞지 않는다.
 */
@Component
@RequiredArgsConstructor
public class PollutantMaterializer {

	private final PollutantRepository pollutantRepository;
	private final PollutantCatalogRepository pollutantCatalogRepository;
	private final PollutantValidator pollutantValidator;

	/**
	 * 측정물질 참조를 pollutantId로 해석한다. 아직 채택하지 않았으면 행을 만든다(멱등).
	 *
	 * <p>카탈로그 지정에 code가 아니라 id를 쓰는 이유: code는 측정분야 안에서만 유일해서
	 * (대기 납과 수질 납이 모두 {@code PB}) code만으로는 물질이 특정되지 않는다.
	 * 클라이언트는 선택 목록에서 이미 {@code catalogId}를 받는다.
	 *
	 * @param pollutantId 이미 이 tenant에 존재하는 측정물질 id. 있으면 그대로 쓴다
	 * @param catalogId   가이드 항목 id. pollutantId가 없을 때 사용한다
	 */
	public Long resolvePollutantId(Long pollutantId, Long catalogId, Long tenantId) {
		if (pollutantId != null) return pollutantId;
		if (catalogId == null) {
			throw new CustomException(ErrorCode.POLLUTANT_REFERENCE_REQUIRED);
		}

		// 존재하지 않는 카탈로그면 여기서 POLLUTANT_CATALOG_NOT_FOUND로 걸러진다.
		PollutantCatalog catalog = pollutantCatalogRepository.findById(catalogId);

		Pollutant existing = pollutantRepository.findByCatalogIdOrNull(catalog.getId(), tenantId);
		if (existing != null) return existing.getId();

		// 폐지 검사는 재사용 분기 뒤에 둔다 — 이미 쓰던 물질은 계속 등록할 수 있어야 한다.
		pollutantValidator.requireSelectable(catalog);

		return createFromCatalog(catalog, tenantId);
	}

	/**
	 * 가이드 항목을 채택한 행을 만든다. 국문명은 카탈로그에서 복사하고 영문명·장비·시험방법은 비워 둔다 —
	 * 이 값들은 고객사가 직접 입력해 관리한다. 측정분야·측정방법·형태는 컬럼이 아니라 카탈로그 조인으로 따라간다.
	 */
	private Long createFromCatalog(PollutantCatalog catalog, Long tenantId) {
		try {
			return pollutantRepository.save(Pollutant.fromCatalog(tenantId, catalog)).getId();
		} catch (DataIntegrityViolationException e) {
			// 같은 (tenant, catalog)를 동시에 등록한 요청이 있었다. 유니크 제약이 막았으므로 그쪽 행을 쓴다.
			Pollutant created = pollutantRepository.findByCatalogIdOrNull(catalog.getId(), tenantId);
			if (created == null) throw e;
			return created.getId();
		}
	}
}
