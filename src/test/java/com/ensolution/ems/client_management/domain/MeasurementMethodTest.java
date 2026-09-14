package com.ensolution.ems.client_management.domain;

import com.ensolution.ems.global.common.enums.SampleGrouping;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 측정방법 도메인 불변식과 수정 시맨틱을 고정한다.
 *
 * <p>통칭 시료명은 "한 병으로 함께 채취"({@code MERGED})의 표기이므로 채취 단위와 어긋나면 안 되고,
 * 통칭명·채취시간은 "없음"이 유효한 값이라 수정 시 <b>전체 채택</b>(null = 비움)이어야 비울 수 있다.
 */
class MeasurementMethodTest {

	private static final Long TENANT = 1L;

	private static MeasurementMethod cartridge() {
		return MeasurementMethod.register(TENANT, "카트리지", SampleGrouping.MERGED, "VOCs", 30, 80);
	}

	@Nested
	@DisplayName("채취 단위 불변식")
	class Grouping {

		@Test
		void 통칭_채취는_통칭명이_있어야_한다() {
			assertThatThrownBy(() -> MeasurementMethod.register(TENANT, "카트리지", SampleGrouping.MERGED, null, null, 10))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.MEASUREMENT_METHOD_GROUPING_MISMATCH.getMessage());
		}

		@Test
		void 항목별_채취에는_통칭명을_둘_수_없다() {
			assertThatThrownBy(() -> MeasurementMethod.register(TENANT, "흡수액", SampleGrouping.PER_ITEM, "VOCs", null, 10))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.MEASUREMENT_METHOD_GROUPING_MISMATCH.getMessage());
		}

		@Test
		void 공백_통칭명은_없는_것으로_본다() {
			assertThatCode(() -> MeasurementMethod.register(TENANT, "흡수액", SampleGrouping.PER_ITEM, "  ", null, 10))
				.doesNotThrowAnyException();
		}

		@Test
		void 통칭명은_앞뒤_공백을_지워_저장한다() {
			MeasurementMethod method = MeasurementMethod.register(TENANT, "카트리지", SampleGrouping.MERGED, " VOCs ", null, 10);

			assertThat(method.getMergedSampleName()).isEqualTo("VOCs");
		}
	}

	@Nested
	@DisplayName("수정")
	class Update {

		@Test
		void 이름과_채취_단위는_전달하지_않으면_유지된다() {
			MeasurementMethod updated = cartridge().update(null, null, "VOCs", 40);

			assertThat(updated.getName()).isEqualTo("카트리지");
			assertThat(updated.getSampleGrouping()).isEqualTo(SampleGrouping.MERGED);
			assertThat(updated.getSamplingMinutes()).isEqualTo(40);
		}

		@Test
		void 채취시간은_null을_보내면_비워진다() {
			MeasurementMethod updated = cartridge().update(null, null, "VOCs", null);

			assertThat(updated.getSamplingMinutes()).isNull();
		}

		@Test
		void 통칭_채취를_항목별_채취로_바꾸려면_통칭명도_함께_비워야_한다() {
			MeasurementMethod updated = cartridge().update(null, SampleGrouping.PER_ITEM, null, 30);

			assertThat(updated.getSampleGrouping()).isEqualTo(SampleGrouping.PER_ITEM);
			assertThat(updated.getMergedSampleName()).isNull();
		}

		@Test
		void 통칭명만_비우고_채취_단위를_두면_거부한다() {
			assertThatThrownBy(() -> cartridge().update(null, null, null, 30))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.MEASUREMENT_METHOD_GROUPING_MISMATCH.getMessage());
		}

		@Test
		void 수정은_id와_tenant를_바꾸지_않는다() {
			MeasurementMethod original = cartridge().toBuilder().id(9L).build();

			MeasurementMethod updated = original.update("카트리지(DNPH)", null, "VOCs", 30);

			assertThat(updated.getId()).isEqualTo(9L);
			assertThat(updated.getTenantId()).isEqualTo(TENANT);
			assertThat(updated.getName()).isEqualTo("카트리지(DNPH)");
		}
	}
}
