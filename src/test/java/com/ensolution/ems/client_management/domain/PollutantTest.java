package com.ensolution.ems.client_management.domain;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 가이드(카탈로그)와 고객사 소유값의 경계 검증.
 *
 * <p>핵심은 두 가지다 — 국문명은 채택 시점에 <b>복사</b>되어 이후 카탈로그와 무관해지고,
 * 측정분야·측정방법·형태는 <b>투영</b>되어 카탈로그를 계속 따라간다.
 */
class PollutantTest {

	private static final Long TENANT = 1L;

	private static PollutantCatalog catalog() {
		return PollutantCatalog.builder()
			.id(10L)
			.code("NOX")
			.field(MeasurementField.AIR)
			.nameKr("질소산화물")
			.method(MeasurementMethod.FIELD_MEASUREMENT)
			.phase(PollutantPhase.GAS)
			.sortOrder(200)
			.active(true)
			.build();
	}

	@Nested
	@DisplayName("가이드 항목 채택")
	class Adoption {

		@Test
		@DisplayName("국문명을 주지 않으면 가이드 값을 복사한다")
		void copiesCatalogNameWhenBlank() {
			Pollutant adopted = Pollutant.fromCatalog(TENANT, catalog());

			assertThat(adopted.getNameKr()).isEqualTo("질소산화물");
		}

		@Test
		@DisplayName("나머지 고객사 소유값은 비운 채로 시작한다")
		void leavesOtherOwnedValuesEmpty() {
			Pollutant adopted = Pollutant.fromCatalog(TENANT, catalog());

			assertThat(adopted.getNameEn()).isNull();
			assertThat(adopted.getEquipment()).isNull();
			assertThat(adopted.getTestMethod()).isNull();
		}

		@Test
		@DisplayName("가이드 속성을 투영해 돌려준다")
		void projectsCatalogAttributes() {
			Pollutant adopted = Pollutant.fromCatalog(TENANT, catalog());

			assertThat(adopted.getCatalogId()).isEqualTo(10L);
			assertThat(adopted.getCode()).isEqualTo("NOX");
			assertThat(adopted.getField()).isEqualTo(MeasurementField.AIR);
			assertThat(adopted.getMethod()).isEqualTo(MeasurementMethod.FIELD_MEASUREMENT);
			assertThat(adopted.getPhase()).isEqualTo(PollutantPhase.GAS);
		}

		@Test
		@DisplayName("국문명을 주면 그 값이 가이드 값을 이긴다")
		void tenantNameWins() {
			Pollutant adopted = Pollutant.register(
				TENANT, catalog(), "질소산화물(자사)", "NOx", "자동측정기", "ES 01301.1");

			assertThat(adopted.getNameKr()).isEqualTo("질소산화물(자사)");
			assertThat(adopted.getNameEn()).isEqualTo("NOx");
			assertThat(adopted.getEquipment()).isEqualTo("자동측정기");
			assertThat(adopted.getTestMethod()).isEqualTo("ES 01301.1");
		}

		@Test
		@DisplayName("공백 국문명은 값으로 보지 않고 가이드 값을 쓴다")
		void blankNameFallsBackToCatalog() {
			Pollutant adopted = Pollutant.register(TENANT, catalog(), "  ", null, null, null);

			assertThat(adopted.getNameKr()).isEqualTo("질소산화물");
		}
	}

	@Nested
	@DisplayName("고객사 소유값 수정")
	class Update {

		@Test
		@DisplayName("전달한 값만 바뀐다")
		void updatesGivenFields() {
			Pollutant updated = Pollutant.register(TENANT, catalog(), "질소산화물", "NOx", "장비A", "ES-01")
				.update(null, null, "장비B", null);

			assertThat(updated.getEquipment()).isEqualTo("장비B");
			assertThat(updated.getNameKr()).isEqualTo("질소산화물");
			assertThat(updated.getNameEn()).isEqualTo("NOx");
			assertThat(updated.getTestMethod()).isEqualTo("ES-01");
		}

		@Test
		@DisplayName("공백은 기존 값을 유지한다")
		void blankKeepsOriginal() {
			Pollutant updated = Pollutant.register(TENANT, catalog(), "질소산화물", "NOx", null, null)
				.update("   ", "   ", null, null);

			assertThat(updated.getNameKr()).isEqualTo("질소산화물");
			assertThat(updated.getNameEn()).isEqualTo("NOx");
		}

		@Test
		@DisplayName("가이드 연결과 투영값은 수정으로 바뀌지 않는다")
		void keepsCatalogLinkAndProjection() {
			Pollutant updated = Pollutant.fromCatalog(TENANT, catalog())
				.update("질소산화물(자사)", null, null, null);

			assertThat(updated.getCatalogId()).isEqualTo(10L);
			assertThat(updated.getCode()).isEqualTo("NOX");
			assertThat(updated.getField()).isEqualTo(MeasurementField.AIR);
			assertThat(updated.getMethod()).isEqualTo(MeasurementMethod.FIELD_MEASUREMENT);
			assertThat(updated.getPhase()).isEqualTo(PollutantPhase.GAS);
		}
	}
}
