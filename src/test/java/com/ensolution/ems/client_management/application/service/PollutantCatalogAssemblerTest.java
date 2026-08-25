package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.client_management.application.FakePollutantCatalogRepository;
import com.ensolution.ems.client_management.application.FakePollutantRepository;
import com.ensolution.ems.client_management.application.service.assembler.PollutantCatalogAssembler;
import com.ensolution.ems.client_management.domain.PollutantCatalog;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 지원 물질 가이드에서 "아직 채택하지 않은 후보"를 뽑는 규칙 검증. */
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
	@DisplayName("채택 후보")
	class Candidates {

		@Test
		@DisplayName("이미 채택한 항목은 후보에서 빠진다")
		void excludesAdopted() {
			PollutantCatalog nox = catalogRepository.given("NOX", MeasurementField.AIR, "질소산화물", 200);
			catalogRepository.given("SOX", MeasurementField.AIR, "황산화물", 210);
			pollutantRepository.given(TENANT, nox, null);

			List<PollutantCatalog> candidates = assembler.assembleCandidates(TENANT, null);

			assertThat(candidates).extracting(PollutantCatalog::getCode).containsExactly("SOX");
		}

		@Test
		@DisplayName("고객사가 표기명을 바꿔도 채택 사실은 그대로 인식한다")
		void excludesAdoptedEvenWhenRenamed() {
			PollutantCatalog nox = catalogRepository.given("NOX", MeasurementField.AIR, "질소산화물", 200);
			pollutantRepository.given(TENANT, nox, "질소산화물(자사)");

			assertThat(assembler.assembleCandidates(TENANT, null)).isEmpty();
		}

		@Test
		@DisplayName("폐지된 항목은 후보에 넣지 않는다 — 새로 채택할 수 없다")
		void excludesInactive() {
			catalogRepository.given("NOX", MeasurementField.AIR, "질소산화물", 200);
			catalogRepository.given("PCE", MeasurementField.AIR, "테트라클로로에틸렌", 460, false);

			List<PollutantCatalog> candidates = assembler.assembleCandidates(TENANT, null);

			assertThat(candidates).extracting(PollutantCatalog::getCode).containsExactly("NOX");
		}

		@Test
		@DisplayName("다른 고객사의 채택 현황은 영향을 주지 않는다")
		void isolatesTenant() {
			PollutantCatalog nox = catalogRepository.given("NOX", MeasurementField.AIR, "질소산화물", 200);
			pollutantRepository.given(2L, nox, "다른 회사 표기");

			assertThat(assembler.assembleCandidates(TENANT, null))
				.extracting(PollutantCatalog::getCode).containsExactly("NOX");
		}

		@Test
		@DisplayName("측정분야로 걸러낸다")
		void filtersByField() {
			catalogRepository.given("NOX", MeasurementField.AIR, "질소산화물", 200);
			catalogRepository.given("BOD", MeasurementField.WATER, "생물화학적산소요구량", 700);

			List<PollutantCatalog> candidates = assembler.assembleCandidates(TENANT, MeasurementField.AIR);

			assertThat(candidates).extracting(PollutantCatalog::getCode).containsExactly("NOX");
		}

		@Test
		@DisplayName("고시 순서를 따른다")
		void ordersBySortOrder() {
			catalogRepository.given("SOX", MeasurementField.AIR, "황산화물", 210);
			catalogRepository.given("TSP", MeasurementField.AIR, "먼지", 100);

			assertThat(assembler.assembleCandidates(TENANT, null))
				.extracting(PollutantCatalog::getNameKr).containsExactly("먼지", "황산화물");
		}

		@Test
		@DisplayName("측정방법·형태가 그대로 실려 온다")
		void carriesCatalogAttributes() {
			catalogRepository.given(
				"NOX", MeasurementField.AIR, "질소산화물", 200, true,
				MeasurementMethod.FIELD_MEASUREMENT, PollutantPhase.GAS);

			PollutantCatalog candidate = assembler.assembleCandidates(TENANT, null).getFirst();

			assertThat(candidate.getField()).isEqualTo(MeasurementField.AIR);
			assertThat(candidate.getMethod()).isEqualTo(MeasurementMethod.FIELD_MEASUREMENT);
			assertThat(candidate.getPhase()).isEqualTo(PollutantPhase.GAS);
		}

		@Test
		@DisplayName("항목이 늘어도 조회 횟수는 소스당 한 번으로 고정된다")
		void doesNotQueryPerItem() {
			for (int i = 0; i < 30; i++) {
				PollutantCatalog catalog =
					catalogRepository.given("CODE_" + i, MeasurementField.AIR, "물질" + i, i);
				if (i % 2 == 0) pollutantRepository.given(TENANT, catalog, null);
			}

			assembler.assembleCandidates(TENANT, null);

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
