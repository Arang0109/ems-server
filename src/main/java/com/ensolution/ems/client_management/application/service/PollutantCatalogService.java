package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.create.CreatePollutantCatalogCommand;
import com.ensolution.ems.client_management.application.command.update.UpdatePollutantCatalogCommand;
import com.ensolution.ems.client_management.application.port.out.PollutantCatalogRepository;
import com.ensolution.ems.client_management.application.validator.PollutantCatalogValidator;
import com.ensolution.ems.client_management.domain.PollutantCatalog;
import com.ensolution.ems.global.common.enums.MeasurementField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 고객사에게 지원하는 측정물질 가이드 관리. 플랫폼 운영자만 호출하는 유스케이스이므로 tenant 범위를 다루지 않는다.
 * 다른 모듈이 소비하지 않으므로 인바운드 포트를 두지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PollutantCatalogService {

	private final PollutantCatalogRepository pollutantCatalogRepository;
	private final PollutantCatalogValidator pollutantCatalogValidator;

	public PollutantCatalog createCatalog(CreatePollutantCatalogCommand command) {
		pollutantCatalogValidator.requireUniqueCode(command.field(), command.code());

		return pollutantCatalogRepository.save(PollutantCatalog.register(
			command.code(), command.field(), command.nameKr(),
			command.method(), command.phase(), command.sortOrder()
		));
	}

	public PollutantCatalog updateCatalog(Long catalogId, UpdatePollutantCatalogCommand command) {
		PollutantCatalog catalog = pollutantCatalogRepository.findById(catalogId);

		return pollutantCatalogRepository.save(catalog.update(
			command.field(), command.nameKr(), command.method(),
			command.phase(), command.sortOrder()
		));
	}

	/** 폐지 처리. 이미 채택해 쓰고 있는 tenant의 데이터는 그대로 남는다. */
	public PollutantCatalog deactivateCatalog(Long catalogId) {
		return pollutantCatalogRepository.save(pollutantCatalogRepository.findById(catalogId).deactivate());
	}

	public PollutantCatalog activateCatalog(Long catalogId) {
		return pollutantCatalogRepository.save(pollutantCatalogRepository.findById(catalogId).activate());
	}

	/**
	 * 멱등 확보. 부트스트랩 시드가 사용한다.
	 * 이미 있는 code는 손대지 않는다 — 운영자가 API로 고친 값을 시드가 되돌리면 안 된다.
	 */
	public PollutantCatalog ensureCatalog(CreatePollutantCatalogCommand command) {
		if (pollutantCatalogRepository.existsByFieldAndCode(command.field(), command.code())) {
			return pollutantCatalogRepository.findByFieldAndCode(command.field(), command.code());
		}
		return createCatalog(command);
	}

	@Transactional(readOnly = true)
	public PollutantCatalog getCatalog(Long catalogId) {
		return pollutantCatalogRepository.findById(catalogId);
	}

	@Transactional(readOnly = true)
	public List<PollutantCatalog> getCatalogList(MeasurementField field, boolean includeInactive) {
		return includeInactive
			? pollutantCatalogRepository.findAll(field)
			: pollutantCatalogRepository.findAllActive(field);
	}
}
