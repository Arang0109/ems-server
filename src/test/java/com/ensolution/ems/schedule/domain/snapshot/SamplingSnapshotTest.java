package com.ensolution.ems.schedule.domain.snapshot;

import com.ensolution.ems.schedule.domain.sampling.MeasurementCategory;
import com.ensolution.ems.schedule.domain.sampling.SamplingSheet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 채취 스냅샷의 생성·부분 갱신 규칙 검증.
 *
 * <p>고정하려는 계약은 <b>성적서 기본정보 폼이 칸을 나눠 채워도 다른 칸이 날아가지 않는다</b>는 것이다.
 * 이 폼은 값의 주인이 넷으로 갈려 있어 서버가 나눠 저장하는데, 어느 한쪽이 "미전달 = 지움"으로
 * 동작하면 한 칸만 고쳐도 나머지가 비워진다.
 */
class SamplingSnapshotTest {

	private static SamplingSnapshot filled() {
		return new SamplingSnapshot(
			LocalTime.of(9, 30), LocalTime.of(11, 0),
			"김담당", "이입회",
			List.of(SamplingSheet.builder().category(MeasurementCategory.GAS).build()));
	}

	@Nested
	@DisplayName("create")
	class Create {

		@Test
		void 사업장_원장의_담당자만_채우고_시각과_기록지는_비워둔다() {
			SamplingSnapshot sampling = SamplingSnapshot.create("김담당", "이입회");

			assertThat(sampling.facilityManager()).isEqualTo("김담당");
			assertThat(sampling.samplingWitness()).isEqualTo("이입회");
			assertThat(sampling.samplingStartedAt()).isNull();
			assertThat(sampling.samplingEndedAt()).isNull();
			assertThat(sampling.sheets()).isEmpty();
		}
	}

	@Nested
	@DisplayName("update")
	class Update {

		@Test
		void 전달된_값만_바꾸고_나머지는_유지한다() {
			SamplingSnapshot updated = filled().update(LocalTime.of(10, 0), null, null, null);

			assertThat(updated.samplingStartedAt()).isEqualTo(LocalTime.of(10, 0));
			assertThat(updated.samplingEndedAt()).isEqualTo(LocalTime.of(11, 0));
			assertThat(updated.facilityManager()).isEqualTo("김담당");
			assertThat(updated.samplingWitness()).isEqualTo("이입회");
		}

		@Test
		void 공백_이름은_미전달로_보아_기존_값을_유지한다() {
			SamplingSnapshot updated = filled().update(null, null, "   ", "");

			assertThat(updated.facilityManager()).isEqualTo("김담당");
			assertThat(updated.samplingWitness()).isEqualTo("이입회");
		}

		@Test
		void 기록지는_건드리지_않는다() {
			SamplingSnapshot updated = filled().update(LocalTime.of(10, 0), null, "박담당", null);

			assertThat(updated.sheets()).hasSize(1);
			assertThat(updated.sheets().getFirst().getCategory()).isEqualTo(MeasurementCategory.GAS);
		}
	}

	@Nested
	@DisplayName("withSheets")
	class WithSheets {

		@Test
		void 기록지만_교체하고_시각과_담당자는_유지한다() {
			SamplingSnapshot replaced = filled().withSheets(
				List.of(SamplingSheet.builder().category(MeasurementCategory.DUST).build()));

			assertThat(replaced.sheets()).hasSize(1);
			assertThat(replaced.sheets().getFirst().getCategory()).isEqualTo(MeasurementCategory.DUST);
			assertThat(replaced.samplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
			assertThat(replaced.facilityManager()).isEqualTo("김담당");
		}
	}
}
