package com.ensolution.ems.schedule.domain.history;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이행 판정 규칙 검증.
 * 미이행을 기한 전(PENDING)과 기한 경과(OVERDUE)로 가르는 것, 그리고 부분 이행이 월 2회 주기에서만
 * 나온다는 것이 핵심 계약이다.
 */
class FulfillmentStatusTest {

	private static final LocalDate BEFORE_DUE = LocalDate.of(2026, 3, 10);
	private static final LocalDate AFTER_DUE = LocalDate.of(2026, 4, 1);

	private static MeasurementPeriod march(MeasurementCycle cycle) {
		return MeasurementPeriod.of(cycle, LocalDate.of(2026, 3, 5));
	}

	@Nested
	@DisplayName("구간당 1회를 요구하는 주기")
	class Once {

		private final MeasurementPeriod period = march(MeasurementCycle.MONTHLY);

		@Test
		@DisplayName("한 번 측정하면 이행이다")
		void fulfilled() {
			assertThat(FulfillmentStatus.of(period, 1, BEFORE_DUE)).isEqualTo(FulfillmentStatus.FULFILLED);
		}

		@Test
		@DisplayName("필요 횟수를 넘겨 측정해도 이행이다")
		void fulfilledWhenExceeded() {
			assertThat(FulfillmentStatus.of(period, 3, BEFORE_DUE)).isEqualTo(FulfillmentStatus.FULFILLED);
		}

		@Test
		@DisplayName("기한 전 미측정은 예정이지 미이행 경고가 아니다")
		void pendingBeforeDue() {
			assertThat(FulfillmentStatus.of(period, 0, BEFORE_DUE)).isEqualTo(FulfillmentStatus.PENDING);
		}

		@Test
		@DisplayName("기한이 지나도록 측정하지 않으면 경과다")
		void overdueAfterDue() {
			assertThat(FulfillmentStatus.of(period, 0, AFTER_DUE)).isEqualTo(FulfillmentStatus.OVERDUE);
		}

		@Test
		@DisplayName("이행한 구간은 기한이 지나도 이행으로 남는다")
		void staysFulfilledAfterDue() {
			assertThat(FulfillmentStatus.of(period, 1, AFTER_DUE)).isEqualTo(FulfillmentStatus.FULFILLED);
		}
	}

	@Nested
	@DisplayName("월 2회 주기 — 부분 이행이 나오는 유일한 경우")
	class Twice {

		private final MeasurementPeriod period = march(MeasurementCycle.TWICE_MONTHLY);

		@Test
		@DisplayName("한 번만 측정하면 부분 이행이다")
		void partial() {
			assertThat(FulfillmentStatus.of(period, 1, BEFORE_DUE)).isEqualTo(FulfillmentStatus.PARTIAL);
		}

		@Test
		@DisplayName("두 번 측정해야 이행이다")
		void fulfilledOnSecond() {
			assertThat(FulfillmentStatus.of(period, 2, BEFORE_DUE)).isEqualTo(FulfillmentStatus.FULFILLED);
		}

		@Test
		@DisplayName("기한이 지난 부분 이행은 조치 대상이므로 경과로 넘긴다")
		void overdueWhenPartialAfterDue() {
			assertThat(FulfillmentStatus.of(period, 1, AFTER_DUE)).isEqualTo(FulfillmentStatus.OVERDUE);
		}
	}

	@Test
	@DisplayName("조치가 필요한 상태는 이행을 제외한 전부다")
	void needsAction() {
		assertThat(FulfillmentStatus.FULFILLED.needsAction()).isFalse();
		assertThat(FulfillmentStatus.PARTIAL.needsAction()).isTrue();
		assertThat(FulfillmentStatus.PENDING.needsAction()).isTrue();
		assertThat(FulfillmentStatus.OVERDUE.needsAction()).isTrue();
	}

	@Test
	@DisplayName("기준일이 없으면 기한을 따지지 않는다")
	void treatsNullBaseDateAsPending() {
		assertThat(FulfillmentStatus.of(march(MeasurementCycle.MONTHLY), 0, null))
			.isEqualTo(FulfillmentStatus.PENDING);
	}
}
