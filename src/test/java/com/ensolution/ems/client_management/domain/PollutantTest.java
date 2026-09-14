package com.ensolution.ems.client_management.domain;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMode;
import com.ensolution.ems.global.common.enums.PollutantPhase;
import com.ensolution.ems.global.common.enums.SampleGrouping;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 가이드(카탈로그)와 고객사 소유값의 경계 검증.
 *
 * <p>핵심은 세 가지다 — 국문명은 채택 시점에 <b>복사</b>되어 이후 카탈로그와 무관해지고,
 * 측정분야·형태는 <b>투영</b>되어 카탈로그를 계속 따라가며, 측정방법은 <b>고객사 소유값</b>이라
 * 채택 시 전달한 값이 저장되고 카탈로그와 무관하다(같은 물질도 업체마다 측정방법이 다를 수 있다).
 * 측정방법은 tenant 소유 애그리거트이므로 여기서는 id로만 다룬다 — 이름·채취 단위 같은 투영값은
 * 어댑터가 조인으로 채운다.
 */
class PollutantTest {

	private static final Long TENANT = 1L;
	private static final Long OTHER_TENANT = 2L;
	private static final Long FIELD_MEASUREMENT = 4L;
	private static final Long TEDLAR_BAG = 7L;
	private static final Long CARTRIDGE = 8L;

	private static PollutantCatalog catalog() {
		return PollutantCatalog.builder()
			.id(10L)
			.code("NOX")
			.field(MeasurementField.AIR)
			.nameKr("질소산화물")
			.phase(PollutantPhase.GAS)
			.mode(MeasurementMode.DIRECT_READING)
			.sortOrder(200)
			.active(true)
			.build();
	}

	/** 고객사 입력 없이 측정방법만 정해 채택한 행. */
	private static Pollutant adopt() {
		return Pollutant.register(TENANT, catalog(), FIELD_MEASUREMENT, null, null, null, null, null);
	}

	@Nested
	@DisplayName("가이드 항목 채택")
	class Adoption {

		@Test
		@DisplayName("국문명을 주지 않으면 가이드 값을 복사한다")
		void copiesCatalogNameWhenBlank() {
			Pollutant adopted = adopt();

			assertThat(adopted.getNameKr()).isEqualTo("질소산화물");
		}

		@Test
		@DisplayName("나머지 고객사 소유값은 비운 채로 시작한다")
		void leavesOtherOwnedValuesEmpty() {
			Pollutant adopted = adopt();

			assertThat(adopted.getNameEn()).isNull();
			assertThat(adopted.getEquipment()).isNull();
			assertThat(adopted.getTestMethod()).isNull();
		}

		@Test
		@DisplayName("가이드 속성을 투영해 돌려준다")
		void projectsCatalogAttributes() {
			Pollutant adopted = adopt();

			assertThat(adopted.getCatalogId()).isEqualTo(10L);
			assertThat(adopted.getCode()).isEqualTo("NOX");
			assertThat(adopted.getField()).isEqualTo(MeasurementField.AIR);
			assertThat(adopted.getPhase()).isEqualTo(PollutantPhase.GAS);
			assertThat(adopted.getMode()).isEqualTo(MeasurementMode.DIRECT_READING);
		}

		@Test
		@DisplayName("측정방법은 채택 시 전달한 값이 저장된다")
		void 측정방법은_채택_시_전달한_값이_저장된다() {
			Pollutant adopted = Pollutant.register(TENANT, catalog(), TEDLAR_BAG, null, null, null, null, null);

			assertThat(adopted.getMethodId()).isEqualTo(TEDLAR_BAG);
		}

		@Test
		@DisplayName("같은 가이드 항목이라도 고객사마다 다른 측정방법을 가질 수 있다")
		void 같은_가이드_항목이라도_고객사마다_다른_측정방법을_가질_수_있다() {
			Pollutant byTedlarBag = Pollutant.register(TENANT, catalog(), TEDLAR_BAG, null, null, null, null, null);
			Pollutant byCartridge = Pollutant.register(OTHER_TENANT, catalog(), CARTRIDGE, null, null, null, null, null);

			assertThat(byTedlarBag.getCatalogId()).isEqualTo(byCartridge.getCatalogId());
			assertThat(byTedlarBag.getMethodId()).isEqualTo(TEDLAR_BAG);
			assertThat(byCartridge.getMethodId()).isEqualTo(CARTRIDGE);
		}

		@Test
		@DisplayName("국문명을 주면 그 값이 가이드 값을 이긴다")
		void tenantNameWins() {
			Pollutant adopted = Pollutant.register(TENANT, catalog(), FIELD_MEASUREMENT, null,
				"질소산화물(자사)", "NOx", "자동측정기", "ES 01301.1");

			assertThat(adopted.getNameKr()).isEqualTo("질소산화물(자사)");
			assertThat(adopted.getNameEn()).isEqualTo("NOx");
			assertThat(adopted.getEquipment()).isEqualTo("자동측정기");
			assertThat(adopted.getTestMethod()).isEqualTo("ES 01301.1");
		}

		@Test
		@DisplayName("공백 국문명은 값으로 보지 않고 가이드 값을 쓴다")
		void blankNameFallsBackToCatalog() {
			Pollutant adopted = Pollutant.register(TENANT, catalog(), FIELD_MEASUREMENT, null, "  ", null, null, null);

			assertThat(adopted.getNameKr()).isEqualTo("질소산화물");
		}
	}

	@Nested
	@DisplayName("유효 채취시간")
	class EffectiveSamplingMinutes {

		/** 어댑터가 조인으로 채우는 측정방법 투영값을 흉내낸다. */
		private Pollutant withMethod(Pollutant pollutant, SampleGrouping grouping, Integer methodMinutes) {
			return pollutant.toBuilder().sampleGrouping(grouping).methodSamplingMinutes(methodMinutes).build();
		}

		@Test
		void 오버라이드가_없으면_방법의_기본값을_쓴다() {
			Pollutant p = withMethod(adopt(), SampleGrouping.PER_ITEM, 40);

			assertThat(p.getEffectiveSamplingMinutes()).isEqualTo(40);
		}

		@Test
		void 항목별_채취는_오버라이드가_방법_기본값을_이긴다() {
			Pollutant p = withMethod(adopt().update(null, 60, null, null, null, null), SampleGrouping.PER_ITEM, 40);

			assertThat(p.getEffectiveSamplingMinutes()).isEqualTo(60);
		}

		@Test
		void 통칭_채취는_오버라이드가_남아_있어도_방법_값만_본다() {
			// 방법을 나중에 MERGED 로 바꾸면 이전 오버라이드가 남을 수 있다 — 한 병인데 항목마다 시간이 다르면 안 된다.
			Pollutant p = withMethod(adopt().update(null, 60, null, null, null, null), SampleGrouping.MERGED, 30);

			assertThat(p.getEffectiveSamplingMinutes()).isEqualTo(30);
		}

		@Test
		void 측정방법이_없는_레거시_행은_오버라이드만_본다() {
			Pollutant p = Pollutant.register(TENANT, catalog(), null, 25, null, null, null, null);

			assertThat(p.getEffectiveSamplingMinutes()).isEqualTo(25);
		}

		@Test
		void 오버라이드는_null을_보내면_걷힌다() {
			Pollutant p = adopt().update(null, 60, null, null, null, null).update(null, null, null, null, null, null);

			assertThat(p.getSamplingMinutes()).isNull();
		}
	}

	@Nested
	@DisplayName("고객사 소유값 수정")
	class Update {

		@Test
		@DisplayName("전달한 값만 바뀐다")
		void updatesGivenFields() {
			Pollutant updated = Pollutant.register(TENANT, catalog(), FIELD_MEASUREMENT, null, "질소산화물", "NOx", "장비A", "ES-01")
				.update(null, null, null, null, "장비B", null);

			assertThat(updated.getEquipment()).isEqualTo("장비B");
			assertThat(updated.getNameKr()).isEqualTo("질소산화물");
			assertThat(updated.getNameEn()).isEqualTo("NOx");
			assertThat(updated.getTestMethod()).isEqualTo("ES-01");
		}

		@Test
		@DisplayName("공백은 기존 값을 유지한다")
		void blankKeepsOriginal() {
			Pollutant updated = Pollutant.register(TENANT, catalog(), FIELD_MEASUREMENT, null, "질소산화물", "NOx", null, null)
				.update(null, null, "   ", "   ", null, null);

			assertThat(updated.getNameKr()).isEqualTo("질소산화물");
			assertThat(updated.getNameEn()).isEqualTo("NOx");
		}

		@Test
		@DisplayName("측정방법을 전달하면 바뀐다")
		void 측정방법을_전달하면_바뀐다() {
			Pollutant updated = adopt().update(CARTRIDGE, null, null, null, null, null);

			assertThat(updated.getMethodId()).isEqualTo(CARTRIDGE);
		}

		@Test
		@DisplayName("측정방법을 전달하지 않으면 유지된다")
		void 측정방법을_전달하지_않으면_유지된다() {
			Pollutant updated = adopt().update(null, null, "질소산화물(자사)", null, null, null);

			assertThat(updated.getMethodId()).isEqualTo(FIELD_MEASUREMENT);
		}

		@Test
		@DisplayName("가이드 연결과 투영값은 수정으로 바뀌지 않는다")
		void keepsCatalogLinkAndProjection() {
			Pollutant updated = adopt().update(null, null, "질소산화물(자사)", null, null, null);

			assertThat(updated.getCatalogId()).isEqualTo(10L);
			assertThat(updated.getCode()).isEqualTo("NOX");
			assertThat(updated.getField()).isEqualTo(MeasurementField.AIR);
			assertThat(updated.getPhase()).isEqualTo(PollutantPhase.GAS);
		}
	}
}
