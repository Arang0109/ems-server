package com.ensolution.ems.schedule.domain.snapshot;

import com.ensolution.ems.global.common.enums.MeasurementField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 측정계획 기본 정보 스냅샷의 생성·병합 규칙 검증.
 * 메타 수정(PUT /api/schedules/{id})이 성적서 발행 단계 입력값을 덮어쓰지 않는다는 계약이 핵심이다.
 */
class BasicInfoTest {

	/** 성적서 발행 단계까지 모두 채워진 기본 정보. */
	private static BasicInfo issued() {
		return new BasicInfo(
			"REF-001",
			"김담당",
			"이입회",
			"박분석",
			"최기술",
			LocalDate.of(2026, 5, 1),
			LocalDate.of(2026, 5, 2),
			LocalDate.of(2026, 5, 4),
			LocalDate.of(2026, 5, 8),
			LocalTime.of(9, 30),
			LocalTime.of(11, 0),
			MeasurementField.AIR,
			"자가측정용");
	}

	@Nested
	@DisplayName("create")
	class Create {

		@Test
		void 메타_파생_필드와_원장_담당자만_채우고_나머지는_비워둔다() {
			BasicInfo info = BasicInfo.create("REF-001", LocalDate.of(2026, 5, 1),
				MeasurementField.AIR, "자가측정용", "김담당", "박입회");

			assertThat(info.referenceNumber()).isEqualTo("REF-001");
			assertThat(info.sampledAt()).isEqualTo(LocalDate.of(2026, 5, 1));
			assertThat(info.measurementField()).isEqualTo(MeasurementField.AIR);
			assertThat(info.schedulePurpose()).isEqualTo("자가측정용");
			assertThat(info.facilityManager()).isEqualTo("김담당");
			assertThat(info.samplingWitness()).isEqualTo("박입회");

			assertThat(info.analyst()).isNull();
			assertThat(info.technicalManager()).isNull();
			assertThat(info.receivedAt()).isNull();
			assertThat(info.analyzedAt()).isNull();
			assertThat(info.issuedAt()).isNull();
			assertThat(info.samplingStartedAt()).isNull();
			assertThat(info.samplingEndedAt()).isNull();
		}
	}

	@Nested
	@DisplayName("applyMeta")
	class ApplyMeta {

		@Test
		void 메타_파생_필드는_갱신된다() {
			BasicInfo merged = issued().applyMeta("REF-002", LocalDate.of(2026, 6, 10),
				MeasurementField.WATER, "기타참고용");

			assertThat(merged.referenceNumber()).isEqualTo("REF-002");
			assertThat(merged.sampledAt()).isEqualTo(LocalDate.of(2026, 6, 10));
			assertThat(merged.measurementField()).isEqualTo(MeasurementField.WATER);
			assertThat(merged.schedulePurpose()).isEqualTo("기타참고용");
		}

		@Test
		void 성적서_발행_단계_입력값은_유실되지_않는다() {
			BasicInfo merged = issued().applyMeta("REF-002", LocalDate.of(2026, 6, 10),
				MeasurementField.WATER, "기타참고용");

			assertThat(merged.facilityManager()).isEqualTo("김담당");
			assertThat(merged.samplingWitness()).isEqualTo("이입회");
			assertThat(merged.analyst()).isEqualTo("박분석");
			assertThat(merged.technicalManager()).isEqualTo("최기술");
			assertThat(merged.receivedAt()).isEqualTo(LocalDate.of(2026, 5, 2));
			assertThat(merged.analyzedAt()).isEqualTo(LocalDate.of(2026, 5, 4));
			assertThat(merged.issuedAt()).isEqualTo(LocalDate.of(2026, 5, 8));
			assertThat(merged.samplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
			assertThat(merged.samplingEndedAt()).isEqualTo(LocalTime.of(11, 0));
		}

		@Test
		void 전달되지_않은_메타_필드는_기존_값을_유지한다() {
			BasicInfo merged = issued().applyMeta(null, null, null, null);

			assertThat(merged.referenceNumber()).isEqualTo("REF-001");
			assertThat(merged.sampledAt()).isEqualTo(LocalDate.of(2026, 5, 1));
			assertThat(merged.measurementField()).isEqualTo(MeasurementField.AIR);
			assertThat(merged.schedulePurpose()).isEqualTo("자가측정용");
		}

		@Test
		void 공백_문자열은_기존_값을_유지한다() {
			BasicInfo merged = issued().applyMeta("   ", null, null, " ");

			assertThat(merged.referenceNumber()).isEqualTo("REF-001");
			assertThat(merged.schedulePurpose()).isEqualTo("자가측정용");
		}
	}

	@Nested
	@DisplayName("update")
	class Update {

		@Test
		void 담당자와_일자_채취시각을_갱신한다() {
			BasicInfo updated = issued().update(
				"새담당", "새입회", "새분석", "새기술",
				LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 4), LocalDate.of(2026, 7, 8),
				LocalTime.of(13, 0), LocalTime.of(15, 30));

			assertThat(updated.facilityManager()).isEqualTo("새담당");
			assertThat(updated.samplingWitness()).isEqualTo("새입회");
			assertThat(updated.analyst()).isEqualTo("새분석");
			assertThat(updated.technicalManager()).isEqualTo("새기술");
			assertThat(updated.receivedAt()).isEqualTo(LocalDate.of(2026, 7, 2));
			assertThat(updated.analyzedAt()).isEqualTo(LocalDate.of(2026, 7, 4));
			assertThat(updated.issuedAt()).isEqualTo(LocalDate.of(2026, 7, 8));
			assertThat(updated.samplingStartedAt()).isEqualTo(LocalTime.of(13, 0));
			assertThat(updated.samplingEndedAt()).isEqualTo(LocalTime.of(15, 30));
		}

		@Test
		void 메타에서_파생되는_필드는_변경하지_않는다() {
			BasicInfo updated = issued().update(
				"새담당", "새입회", "새분석", "새기술",
				LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 4), LocalDate.of(2026, 7, 8),
				LocalTime.of(13, 0), LocalTime.of(15, 30));

			assertThat(updated.referenceNumber()).isEqualTo("REF-001");
			assertThat(updated.sampledAt()).isEqualTo(LocalDate.of(2026, 5, 1));
			assertThat(updated.measurementField()).isEqualTo(MeasurementField.AIR);
			assertThat(updated.schedulePurpose()).isEqualTo("자가측정용");
		}

		@Test
		void 전달되지_않은_필드는_기존_값을_유지한다() {
			BasicInfo updated = issued().update(null, null, null, null, null, null, null, null, null);

			assertThat(updated).isEqualTo(issued());
		}

		@Test
		void 공백_문자열은_기존_담당자를_유지한다() {
			BasicInfo updated = issued().update("  ", "", null, " ", null, null, null, null, null);

			assertThat(updated.facilityManager()).isEqualTo("김담당");
			assertThat(updated.samplingWitness()).isEqualTo("이입회");
			assertThat(updated.analyst()).isEqualTo("박분석");
			assertThat(updated.technicalManager()).isEqualTo("최기술");
		}

		@Test
		void 일부만_전달하면_나머지는_그대로다() {
			BasicInfo updated = issued().update(null, null, null, null, null, null,
				LocalDate.of(2026, 7, 8), null, null);

			assertThat(updated.issuedAt()).isEqualTo(LocalDate.of(2026, 7, 8));
			assertThat(updated.receivedAt()).isEqualTo(LocalDate.of(2026, 5, 2));
			assertThat(updated.analyzedAt()).isEqualTo(LocalDate.of(2026, 5, 4));
			assertThat(updated.samplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
		}
	}
}
