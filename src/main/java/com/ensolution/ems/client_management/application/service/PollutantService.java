package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.command.create.CreatePollutantCommand;
import com.ensolution.ems.client_management.application.command.list_item.PollutantListItem;
import com.ensolution.ems.client_management.application.command.update.UpdatePollutantCommand;
import com.ensolution.ems.client_management.application.service.assembler.PollutantCatalogAssembler;
import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.client_management.domain.PollutantCatalog;
import com.ensolution.ems.client_management.application.port.out.PollutantCatalogRepository;
import com.ensolution.ems.client_management.application.port.out.PollutantRepository;
import com.ensolution.ems.client_management.application.validator.PollutantValidator;
import com.ensolution.ems.global.common.enums.MeasurementField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PollutantService {

	private final PollutantRepository pollutantRepository;
	private final PollutantCatalogRepository pollutantCatalogRepository;
	private final PollutantCatalogAssembler pollutantCatalogAssembler;
	private final PollutantValidator pollutantValidator;

	/** 가이드 항목을 채택한다. 가이드에 없는 물질은 만들 수 없으므로 {@code catalogId}는 필수다. */
	public Pollutant createPollutant(CreatePollutantCommand command) {
		// 존재하지 않는 카탈로그면 여기서 POLLUTANT_CATALOG_NOT_FOUND로 걸러진다.
		PollutantCatalog catalog = pollutantCatalogRepository.findById(command.catalogId());

		pollutantValidator.requireSelectable(catalog);
		pollutantValidator.requireCatalogNotLinked(catalog.getId(), command.tenantId());

		return pollutantRepository.save(Pollutant.register(
			command.tenantId(), catalog,
			command.nameKr(), command.nameEn(), command.equipment(), command.testMethod()
		));
	}

	/** 고객사 소유값만 수정한다. 측정분야·측정방법·형태는 카탈로그 소유이므로 대상이 아니다. */
	public Pollutant updatePollutant(Long id, Long tenantId, UpdatePollutantCommand command) {
		Pollutant pollutant = pollutantRepository.findById(id, tenantId);

		return pollutantRepository.save(pollutant.update(
			command.nameKr(), command.nameEn(), command.equipment(), command.testMethod()
		));
	}

	public void deletePollutant(Long id, Long tenantId) {
		pollutantRepository.deleteById(id, tenantId);
	}

	/**
	 * 선택 가능한 측정물질 목록.
	 *
	 * @param includeCatalog true면 아직 채택하지 않은 가이드 항목까지 포함한다(기본).
	 *                       false면 이 tenant가 채택한 것만 돌려준다
	 */
	@Transactional(readOnly = true)
	public List<PollutantListItem> getPollutantList(MeasurementField field, Long tenantId, boolean includeCatalog) {
		List<PollutantListItem> items = pollutantCatalogAssembler.assembleSelectable(tenantId, field);
		if (includeCatalog) return items;

		return items.stream().filter(item -> item.pollutantId() != null).toList();
	}

	/**
	 * 단건 조회. 고객사 소유값에 카탈로그 투영값(code·측정분야·측정방법·형태)이 이미 채워진 상태로 돌아오므로
	 * 목록과 상세가 같은 값을 보여 준다.
	 */
	@Transactional(readOnly = true)
	public Pollutant getPollutant(Long id, Long tenantId) {
		return pollutantRepository.findById(id, tenantId);
	}
}
