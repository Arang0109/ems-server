package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.FakePollutantCatalogRepository;
import com.ensolution.ems.client_management.application.FakePollutantRepository;
import com.ensolution.ems.client_management.application.validator.PollutantValidator;
import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.client_management.domain.PollutantCatalog;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 가이드 항목을 시설에 등록할 때 고객사 측정물질 행을 만들어 주는 규칙 검증.
 * 같은 물질을 반복 선택해도 행이 하나만 생겨야 한다.
 */
class PollutantMaterializerTest {

	private static final Long TENANT = 1L;

	private FakePollutantRepository pollutantRepository;
	private FakePollutantCatalogRepository catalogRepository;
	private PollutantMaterializer materializer;

	@BeforeEach
	void setUp() {
		pollutantRepository = new FakePollutantRepository();
		catalogRepository = new FakePollutantCatalogRepository();
		materializer = new PollutantMaterializer(
			pollutantRepository, catalogRepository, new PollutantValidator(pollutantRepository));
	}

	@Test
	@DisplayName("측정물질 id가 오면 그대로 쓴다")
	void passesThroughPollutantId() {
		Long resolved = materializer.resolvePollutantId(500L, null, TENANT);


		assertThat(resolved).isEqualTo(500L);
		assertThat(pollutantRepository.saveCount()).isZero();
	}

	@Test
	@DisplayName("카탈로그만 지정하면 국문명을 복사한 측정물질을 새로 만든다")
	void materializesFromCatalog() {
		PollutantCatalog nox = catalogRepository.given("NOX", MeasurementField.AIR, "질소산화물", 200);

		Long resolved = materializer.resolvePollutantId(null, nox.getId(), TENANT);

		assertThat(pollutantRepository.saveCount()).isEqualTo(1);

		Pollutant created = pollutantRepository.findById(resolved, TENANT);
		assertThat(created.getCatalogId()).isEqualTo(nox.getId());
		// 국문명은 고객사 소유값이므로 채택 시점에 복사한다
		assertThat(created.getNameKr()).isEqualTo("질소산화물");
		// 나머지 고객사 소유값은 비워 두고 직접 입력받는다
		assertThat(created.getNameEn()).isNull();
		assertThat(created.getEquipment()).isNull();
		assertThat(created.getTestMethod()).isNull();
		// 측정분야는 카탈로그 투영값이므로 따라온다
		assertThat(created.getField()).isEqualTo(MeasurementField.AIR);
	}

	@Test
	@DisplayName("폐지된 물질은 새로 채택할 수 없다")
	void rejectsInactiveCatalog() {
		PollutantCatalog retired =
			catalogRepository.given("PCE", MeasurementField.AIR, "테트라클로로에틸렌", 460, false);

		assertThatThrownBy(() -> materializer.resolvePollutantId(null, retired.getId(), TENANT))
			.isInstanceOf(CustomException.class)
			.hasMessage(ErrorCode.POLLUTANT_CATALOG_INACTIVE.getMessage());
		assertThat(pollutantRepository.saveCount()).isZero();
	}

	@Test
	@DisplayName("이미 채택해 쓰던 물질이 폐지돼도 계속 등록할 수 있다")
	void allowsInactiveWhenAlreadyAdopted() {
		PollutantCatalog retired =
			catalogRepository.given("PCE", MeasurementField.AIR, "테트라클로로에틸렌", 460, false);
		Long existing = pollutantRepository.given(TENANT, retired, null).getId();

		assertThat(materializer.resolvePollutantId(null, retired.getId(), TENANT)).isEqualTo(existing);
		assertThat(pollutantRepository.saveCount()).isZero();
	}

	@Test
	@DisplayName("같은 물질을 다시 고르면 기존 측정물질을 재사용한다")
	void reusesExisting() {
		PollutantCatalog sox = catalogRepository.given("SOX", MeasurementField.AIR, "황산화물", 210);

		Long first = materializer.resolvePollutantId(null, sox.getId(), TENANT);
		Long second = materializer.resolvePollutantId(null, sox.getId(), TENANT);

		assertThat(second).isEqualTo(first);
		assertThat(pollutantRepository.saveCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("대기 납과 수질 납은 code가 같아도 서로 다른 측정물질이 된다")
	void separatesSameCodeAcrossFields() {
		PollutantCatalog airLead = catalogRepository.given("PB", MeasurementField.AIR, "납", 500);
		PollutantCatalog waterLead = catalogRepository.given("PB", MeasurementField.WATER, "납", 900);

		Long air = materializer.resolvePollutantId(null, airLead.getId(), TENANT);
		Long water = materializer.resolvePollutantId(null, waterLead.getId(), TENANT);

		assertThat(water).isNotEqualTo(air);
		assertThat(pollutantRepository.saveCount()).isEqualTo(2);
	}

	@Test
	@DisplayName("고객사마다 각자의 측정물질이 만들어진다")
	void materializesPerTenant() {
		PollutantCatalog sox = catalogRepository.given("SOX", MeasurementField.AIR, "황산화물", 210);

		Long mine = materializer.resolvePollutantId(null, sox.getId(), TENANT);
		Long theirs = materializer.resolvePollutantId(null, sox.getId(), 2L);

		assertThat(theirs).isNotEqualTo(mine);
		assertThat(pollutantRepository.saveCount()).isEqualTo(2);
	}

	@Test
	@DisplayName("측정물질 id도 카탈로그 id도 없으면 거부한다")
	void rejectsMissingReference() {
		assertThatThrownBy(() -> materializer.resolvePollutantId(null, null, TENANT))
			.isInstanceOf(CustomException.class)
			.hasMessage(ErrorCode.POLLUTANT_REFERENCE_REQUIRED.getMessage());
	}

	@Test
	@DisplayName("없는 카탈로그는 미존재로 처리한다")
	void rejectsUnknownCatalog() {
		assertThatThrownBy(() -> materializer.resolvePollutantId(null, 9999L, TENANT))
			.isInstanceOf(CustomException.class)
			.hasMessage(ErrorCode.POLLUTANT_CATALOG_NOT_FOUND.getMessage());
	}
}
