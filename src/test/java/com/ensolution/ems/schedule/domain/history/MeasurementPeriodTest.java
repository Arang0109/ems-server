package com.ensolution.ems.schedule.domain.history;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 주기 구간 산출 규칙 검증.
 * 구간 경계가 역년 고정이라는 것과, 월 2회 주기가 구간이 아니라 필요 횟수로 표현된다는 것이 핵심 계약이다.
 */
class MeasurementPeriodTest {

	@Nested
	@DisplayName("구간 산출 — 측정일이 속한 칸을 정한다")
	class Of {

		@ParameterizedTest(name = "{0} 주기의 {1}은 {2}번 구간")
		@CsvSource({
			"MONTHLY,       2026-01-01, 1",
			"MONTHLY,       2026-12-31, 12",
			"TWICE_MONTHLY, 2026-03-15, 3",
			"BIMONTHLY,     2026-01-31, 1",
			"BIMONTHLY,     2026-02-01, 1",
			"BIMONTHLY,     2026-03-01, 2",
			"BIMONTHLY,     2026-12-31, 6",
			"QUARTERLY,     2026-03-31, 1",
			"QUARTERLY,     2026-04-01, 2",
			"QUARTERLY,     2026-12-31, 4",
			"SEMI_ANNUAL,   2026-06-30, 1",
			"SEMI_ANNUAL,   2026-07-01, 2",
			"ANNUAL,        2026-08-18, 1"
		})
		@DisplayName("역년 고정 경계로 구간을 나눈다")
		void resolvesIndex(MeasurementCycle cycle, LocalDate date, int expectedIndex) {
			MeasurementPeriod period = MeasurementPeriod.of(cycle, date);

			assertThat(period.index()).isEqualTo(expectedIndex);
			assertThat(period.year()).isEqualTo(2026);
		}

		@Test
		@DisplayName("주기나 측정일이 없으면 구간을 정할 수 없다")
		void returnsNullWithoutInput() {
			assertThat(MeasurementPeriod.of(null, LocalDate.of(2026, 3, 1))).isNull();
			assertThat(MeasurementPeriod.of(MeasurementCycle.QUARTERLY, null)).isNull();
		}

		@Test
		@DisplayName("구간 번호가 주기의 범위를 벗어나면 만들 수 없다")
		void rejectsOutOfRangeIndex() {
			assertThatThrownBy(() -> new MeasurementPeriod(MeasurementCycle.QUARTERLY, 2026, 5))
				.isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> new MeasurementPeriod(MeasurementCycle.ANNUAL, 2026, 0))
				.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	@DisplayName("연간 구간 — 현황판의 열이 된다")
	class AllIn {

		@ParameterizedTest(name = "{0} 주기는 한 해에 {1}개 구간")
		@CsvSource({
			"MONTHLY, 12", "TWICE_MONTHLY, 12", "BIMONTHLY, 6",
			"QUARTERLY, 4", "SEMI_ANNUAL, 2", "ANNUAL, 1"
		})
		void countsPeriods(MeasurementCycle cycle, int expectedCount) {
			assertThat(MeasurementPeriod.allIn(cycle, 2026)).hasSize(expectedCount);
		}

		@ParameterizedTest
		@EnumSource(MeasurementCycle.class)
		@DisplayName("구간이 1월 1일부터 12월 31일까지 빈틈없이 이어진다")
		void coversWholeYearWithoutGap(MeasurementCycle cycle) {
			var periods = MeasurementPeriod.allIn(cycle, 2026);

			assertThat(periods.getFirst().startDate()).isEqualTo(LocalDate.of(2026, 1, 1));
			assertThat(periods.getLast().endDate()).isEqualTo(LocalDate.of(2026, 12, 31));
			for (int i = 1; i < periods.size(); i++) {
				assertThat(periods.get(i).startDate())
					.isEqualTo(periods.get(i - 1).endDate().plusDays(1));
			}
		}

		@Test
		@DisplayName("주기가 없으면 펼칠 구간도 없다")
		void returnsEmptyWithoutCycle() {
			assertThat(MeasurementPeriod.allIn(null, 2026)).isEmpty();
		}
	}

	@Nested
	@DisplayName("필요 횟수 — 월 2회만 2회이고 나머지는 1회다")
	class RequiredCount {

		@ParameterizedTest
		@EnumSource(value = MeasurementCycle.class, names = "TWICE_MONTHLY", mode = EnumSource.Mode.EXCLUDE)
		void requiresOnce(MeasurementCycle cycle) {
			assertThat(MeasurementPeriod.of(cycle, LocalDate.of(2026, 3, 1)).requiredCount()).isEqualTo(1);
		}

		@Test
		@DisplayName("월 2회 주기는 월 주기와 같은 구간을 쓰되 두 번을 요구한다")
		void requiresTwice() {
			MeasurementPeriod monthly = MeasurementPeriod.of(MeasurementCycle.MONTHLY, LocalDate.of(2026, 3, 5));
			MeasurementPeriod twice = MeasurementPeriod.of(MeasurementCycle.TWICE_MONTHLY, LocalDate.of(2026, 3, 5));

			assertThat(twice.index()).isEqualTo(monthly.index());
			assertThat(twice.startDate()).isEqualTo(monthly.startDate());
			assertThat(twice.requiredCount()).isEqualTo(2);
			assertThat(monthly.requiredCount()).isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("구간 키 — 주기가 바뀌어도 과거 기록이 새 칸에 붙지 않는다")
	class Key {

		@Test
		@DisplayName("주기마다 다른 표식을 쓴다")
		void distinguishesCycles() {
			LocalDate march = LocalDate.of(2026, 3, 5);

			assertThat(MeasurementPeriod.of(MeasurementCycle.MONTHLY, march).key()).isEqualTo("2026-M03");
			assertThat(MeasurementPeriod.of(MeasurementCycle.TWICE_MONTHLY, march).key()).isEqualTo("2026-T03");
			assertThat(MeasurementPeriod.of(MeasurementCycle.BIMONTHLY, march).key()).isEqualTo("2026-B2");
			assertThat(MeasurementPeriod.of(MeasurementCycle.QUARTERLY, march).key()).isEqualTo("2026-Q1");
			assertThat(MeasurementPeriod.of(MeasurementCycle.SEMI_ANNUAL, march).key()).isEqualTo("2026-H1");
			assertThat(MeasurementPeriod.of(MeasurementCycle.ANNUAL, march).key()).isEqualTo("2026-Y");
		}

		@Test
		@DisplayName("같은 달을 가리켜도 주기가 다르면 다른 키다")
		void neverCollidesAcrossCycles() {
			LocalDate march = LocalDate.of(2026, 3, 5);
			String monthly = MeasurementPeriod.of(MeasurementCycle.MONTHLY, march).key();
			String bimonthly = MeasurementPeriod.of(MeasurementCycle.BIMONTHLY, march).key();

			assertThat(monthly).isNotEqualTo(bimonthly);
		}
	}

	@Nested
	@DisplayName("기한 — 구간 종료일이 이행 기한이다")
	class Due {

		@Test
		@DisplayName("기준일이 종료일을 지나야 경과로 본다")
		void detectsOverdue() {
			MeasurementPeriod q1 = MeasurementPeriod.of(MeasurementCycle.QUARTERLY, LocalDate.of(2026, 2, 1));

			assertThat(q1.endDate()).isEqualTo(LocalDate.of(2026, 3, 31));
			assertThat(q1.isOverdueAt(LocalDate.of(2026, 3, 31))).isFalse();
			assertThat(q1.isOverdueAt(LocalDate.of(2026, 4, 1))).isTrue();
			assertThat(q1.isOverdueAt(null)).isFalse();
		}
	}
}
