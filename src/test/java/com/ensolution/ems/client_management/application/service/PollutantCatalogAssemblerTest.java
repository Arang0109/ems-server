package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.FakePollutantCatalogRepository;
import com.ensolution.ems.client_management.application.FakePollutantRepository;
import com.ensolution.ems.client_management.application.command.list_item.PollutantListItem;
import com.ensolution.ems.client_management.application.service.assembler.PollutantCatalogAssembler;
import com.ensolution.ems.client_management.domain.PollutantCatalog;
import com.ensolution.ems.client_management.domain.PollutantSource;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** 지원 물질 가이드와 고객사 채택 현황을 합쳐 "선택 가능한 측정물질"을 만드는 규칙 검증. */
class PollutantCatalogAssemblerTest {

	private static final Long TENANT = 1L;

	private FakePollutantRepository pollutantRepository;
	private FakePollutantCatalogRepository catalogRepository;
	private PollutantCatalogAssembler assembler;

	@BeforeEach
	void setUp() {
		pollutantRepository = new FakePollutantRepository();
		catalogRepository = new FakePollutantCatalogRepository();
		assembler = new PollutantCatalogAssembler(pollutantRepository, catalogRepository);
	}

	@Nested
	@DisplayName("선택 목록")
	class Selectable {

		@Test
		@DisplayName("채택 여부에 따라 출처를 구분한다")
		void classifiesSource() {
			PollutantCatalog nox = catalogRepository.given("NOX", MeasurementField.AIR, "질소산화물", 200);
			catalogRepository.given("SOX", MeasurementField.AIR, "황산화물", 210);
			pollutantRepository.given(TENANT, nox, null);

			List<PollutantListItem> items = assembler.assembleSelectable(TENANT, null);

			assertThat(items)
				.extracting(PollutantListItem::code, PollutantListItem::source)
				.containsExactly(
					tuple("NOX", PollutantSource.REGISTERED),
					tuple("SOX", PollutantSource.CATALOG));
		}

		@Test
		@DisplayName("아직 채택하지 않은 항목은 측정물질 id와 고객사 입력값이 비어 있다")
		void catalogItemHasNoPollutantId() {
			catalogRepository.given("NOX", MeasurementField.AIR, "질소산화물", 200);

			PollutantListItem item = assembler.assembleSelectable(TENANT, null).get(0);

			assertThat(item.pollutantId()).isNull();
			assertThat(item.catalogId()).isNotNull();
			assertThat(item.nameKr()).isEqualTo("질소산화물");
			assertThat(item.nameEn()).isNull();
			assertThat(item.equipment()).isNull();
			assertThat(item.testMethod()).isNull();
		}

		@Test
		@DisplayName("고객사가 바꾼 표기명이 보인다")
		void showsTenantOwnedName() {
			PollutantCatalog nox = catalogRepository.given("NOX", MeasurementField.AIR, "질소산화물", 200);
			pollutantRepository.given(TENANT, nox, "질소산화물(자사)");

			PollutantListItem item = assembler.assembleSelectable(TENANT, null).get(0);

			assertThat(item.nameKr()).isEqualTo("질소산화물(자사)");
			assertThat(item.code()).isEqualTo("NOX");
		}

		@Test
		@DisplayName("채택 시 표기명이 없으면 가이드 국문명이 복사된 상태로 보인다")
		void copiesCatalogNameOnAdoption() {
			PollutantCatalog nox = catalogRepository.given("NOX", MeasurementField.AIR, "질소산화물", 200);
			pollutantRepository.given(TENANT, nox, null);

			PollutantListItem item = assembler.assembleSelectable(TENANT, null).get(0);

			assertThat(item.source()).isEqualTo(PollutantSource.REGISTERED);
			assertThat(item.nameKr()).isEqualTo("질소산화물");
		}

		@Test
		@DisplayName("측정분야·측정방법·형태는 가이드에서 따라온다")
		void projectsCatalogAttributes() {
			PollutantCatalog nox = catalogRepository.given(
				"NOX", MeasurementField.AIR, "질소산화물", 200, true,
				MeasurementMethod.FIELD_MEASUREMENT, PollutantPhase.GAS);
			pollutantRepository.given(TENANT, nox, "질소산화물(자사)");

			PollutantListItem item = assembler.assembleSelectable(TENANT, null).get(0);

			assertThat(item.field()).isEqualTo(MeasurementField.AIR);
			assertThat(item.method()).isEqualTo(MeasurementMethod.FIELD_MEASUREMENT);
			assertThat(item.phase()).isEqualTo(PollutantPhase.GAS);
		}

		@Test
		@DisplayName("다른 고객사의 측정물질은 섞이지 않는다")
		void isolatesTenant() {
			PollutantCatalog nox = catalogRepository.given("NOX", MeasurementField.AIR, "질소산화물", 200);
			pollutantRepository.given(2L, nox, "다른 회사 표기");

			PollutantListItem item = assembler.assembleSelectable(TENANT, null).get(0);

			assertThat(item.source()).isEqualTo(PollutantSource.CATALOG);
			assertThat(item.nameKr()).isEqualTo("질소산화물");
		}

		@Test
		@DisplayName("측정분야로 걸러낸다")
		void filtersByField() {
			catalogRepository.given("NOX", MeasurementField.AIR, "질소산화물", 200);
			catalogRepository.given("BOD", MeasurementField.WATER, "생물화학적산소요구량", 700);

			List<PollutantListItem> items = assembler.assembleSelectable(TENANT, MeasurementField.AIR);

			assertThat(items).extracting(PollutantListItem::code).containsExactly("NOX");
		}

		@Test
		@DisplayName("고시 순서를 따른다")
		void ordersBySortOrder() {
			catalogRepository.given("SOX", MeasurementField.AIR, "황산화물", 210);
			catalogRepository.given("TSP", MeasurementField.AIR, "먼지", 100);

			List<PollutantListItem> items = assembler.assembleSelectable(TENANT, null);

			assertThat(items).extracting(PollutantListItem::nameKr).containsExactly("먼지", "황산화물");
		}

		@Test
		@DisplayName("폐지된 물질은 감추되, 이미 쓰고 있는 고객사에는 계속 보여 준다")
		void keepsInactiveWhenAlreadyUsed() {
			PollutantCatalog retired =
				catalogRepository.given("PCE", MeasurementField.AIR, "테트라클로로에틸렌", 460, false);

			assertThat(assembler.assembleSelectable(TENANT, null)).isEmpty();

			pollutantRepository.given(TENANT, retired, null);

			assertThat(assembler.assembleSelectable(TENANT, null))
				.extracting(PollutantListItem::code, PollutantListItem::source)
				.containsExactly(tuple("PCE", PollutantSource.REGISTERED));
			// 폐지 항목을 위해 항목별로 다시 읽지 않는다
			assertThat(catalogRepository.findByIdCount()).isZero();
		}

		@Test
		@DisplayName("항목이 늘어도 조회 횟수는 소스당 한 번으로 고정된다")
		void doesNotQueryPerItem() {
			for (int i = 0; i < 30; i++) {
				PollutantCatalog catalog =
					catalogRepository.given("CODE_" + i, MeasurementField.AIR, "물질" + i, i);
				if (i % 2 == 0) pollutantRepository.given(TENANT, catalog, null);
			}

			assembler.assembleSelectable(TENANT, null);

			assertThat(catalogRepository.listQueryCount()).isEqualTo(1);
			assertThat(pollutantRepository.listQueryCount()).isEqualTo(1);
			assertThat(catalogRepository.findByIdCount()).isZero();
		}
	}

	@Nested
	@DisplayName("id로 찾는 측정물질")
	class ById {

		@Test
		@DisplayName("고객사 값과 가이드 투영값이 함께 채워진다")
		void returnsOwnedAndProjectedValues() {
			PollutantCatalog nox = catalogRepository.given(
				"NOX", MeasurementField.AIR, "질소산화물", 200, true,
				MeasurementMethod.FIELD_MEASUREMENT, PollutantPhase.GAS);
			Long pollutantId = pollutantRepository.given(TENANT, nox, "질소산화물(자사)").getId();

			var pollutant = assembler.pollutantById(TENANT).get(pollutantId);

			assertThat(pollutant.getNameKr()).isEqualTo("질소산화물(자사)");
			assertThat(pollutant.getCode()).isEqualTo("NOX");
			assertThat(pollutant.getField()).isEqualTo(MeasurementField.AIR);
			assertThat(pollutant.getPhase()).isEqualTo(PollutantPhase.GAS);
		}

		@Test
		@DisplayName("카탈로그를 다시 읽지 않는다 — 투영은 어댑터가 이미 끝냈다")
		void doesNotReadCatalog() {
			PollutantCatalog nox = catalogRepository.given("NOX", MeasurementField.AIR, "질소산화물", 200);
			pollutantRepository.given(TENANT, nox, null);

			assembler.pollutantById(TENANT);

			assertThat(catalogRepository.listQueryCount()).isZero();
			assertThat(catalogRepository.findByIdCount()).isZero();
		}

		@Test
		@DisplayName("측정물질이 없으면 빈 결과를 돌려준다")
		void emptyWhenNone() {
			assertThat(assembler.pollutantById(TENANT)).isEmpty();
		}
	}
}
