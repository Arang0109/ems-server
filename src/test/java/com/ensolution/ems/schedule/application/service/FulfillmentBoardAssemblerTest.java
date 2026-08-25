package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.client_management.application.port.in.StackMeasurementItemSummary;
import com.ensolution.ems.client_management.application.port.in.StackMeasurementSummary;
import com.ensolution.ems.client_management.application.port.in.StackQueryUseCase;
import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.schedule.application.FakeMeasurementRecordRepository;
import com.ensolution.ems.schedule.application.command.detail.FulfillmentBoardDetail;
import com.ensolution.ems.schedule.application.command.list_item.PendingMeasurementListItem;
import com.ensolution.ems.schedule.domain.history.FulfillmentStatus;
import com.ensolution.ems.schedule.domain.history.MeasurementPeriod;
import com.ensolution.ems.schedule.domain.history.MeasurementRecord;
import com.ensolution.ems.schedule.domain.history.MeasurementResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이행 현황판 조립 검증.
 * <b>한 번도 측정하지 않은 항목이 행으로 나오는지</b>가 가장 중요한 계약이다 — 축을 이력에서 뽑으면
 * 미이행 항목이 화면에 아예 등장하지 않아 현황판이 무의미해진다.
 */
class FulfillmentBoardAssemblerTest {

	private static final Long TENANT_ID = 10L;
	private static final Long STACK_ID = 100L;
	private static final int YEAR = 2026;
	/** 3월 중순 — 1·2월은 기한이 지났고 3월은 진행 중, 4월 이후는 아직 오지 않았다. */
	private static final LocalDate TODAY = LocalDate.of(2026, 3, 15);

	private FakeMeasurementRecordRepository repository;
	private FakeStackQueryUseCase stackQuery;
	private FulfillmentBoardAssembler assembler;

	@BeforeEach
	void setUp() {
		repository = new FakeMeasurementRecordRepository();
		stackQuery = new FakeStackQueryUseCase();
		assembler = new FulfillmentBoardAssembler(stackQuery, repository);
	}

	private static StackMeasurementItemSummary itemOf(Long pollutantId, MeasurementCycle cycle) {
		return new StackMeasurementItemSummary(
			1L, "본사공장", STACK_ID, "1호 배출구", MeasurementField.AIR,
			pollutantId * 10, pollutantId, "NOX", "질소산화물", cycle, new BigDecimal("100"));
	}

	private void givenRecord(Long scheduleId, Long pollutantId, MeasurementCycle cycle, LocalDate sampledAt) {
		MeasurementPeriod period = MeasurementPeriod.of(cycle, sampledAt);
		repository.saveAll(List.of(new MeasurementRecord(
			null, TENANT_ID, STACK_ID, scheduleId, pollutantId * 10, pollutantId, "NOX", "질소산화물",
			cycle, sampledAt.getYear(), period == null ? null : period.index(),
			sampledAt, LocalDateTime.now(), MeasurementResult.empty())));
	}

	private FulfillmentBoardDetail assemble() {
		return assembler.assemble(TENANT_ID, null, STACK_ID, YEAR, TODAY);
	}

	private static FulfillmentBoardDetail.Cell cellOf(FulfillmentBoardDetail.Row row, String key) {
		return row.cells().stream()
			.filter(cell -> cell.key().equals(key))
			.findFirst()
			.orElseThrow(() -> new AssertionError("구간 없음: " + key));
	}

	@Nested
	@DisplayName("행 축 — 원장에서 온다")
	class Rows {

		@Test
		@DisplayName("한 번도 측정하지 않은 항목도 행으로 나온다")
		void includesNeverMeasuredItem() {
			stackQuery.items.add(itemOf(11L, MeasurementCycle.QUARTERLY));

			FulfillmentBoardDetail board = assemble();

			assertThat(board.rows()).hasSize(1);
			assertThat(board.rows().getFirst().fulfilledTotal()).isZero();
			assertThat(board.rows().getFirst().cells()).hasSize(4);
		}

		@Test
		@DisplayName("주기가 없는 항목은 행에서 빼되 몇 건인지 알린다")
		void reportsUnscheduledItems() {
			stackQuery.items.add(itemOf(11L, MeasurementCycle.QUARTERLY));
			stackQuery.items.add(itemOf(22L, null));

			FulfillmentBoardDetail board = assemble();

			assertThat(board.rows()).hasSize(1);
			assertThat(board.unscheduledItemCount()).isEqualTo(1);
		}

		@Test
		@DisplayName("연간 필요 횟수는 구간 수와 구간당 필요 횟수의 곱이다")
		void sumsRequiredTotal() {
			stackQuery.items.add(itemOf(11L, MeasurementCycle.TWICE_MONTHLY));

			FulfillmentBoardDetail board = assemble();

			assertThat(board.rows().getFirst().requiredTotal()).isEqualTo(24);
		}
	}

	@Nested
	@DisplayName("셀 판정 — 이행 이력과 결합한다")
	class Cells {

		@Test
		@DisplayName("측정한 구간은 이행, 지나간 빈 구간은 경과, 앞으로 올 구간은 예정이다")
		void marksEachCellByRecordsAndDueDate() {
			stackQuery.items.add(itemOf(11L, MeasurementCycle.MONTHLY));
			givenRecord(1L, 11L, MeasurementCycle.MONTHLY, LocalDate.of(2026, 2, 10));

			FulfillmentBoardDetail.Row nox = assemble().rows().getFirst();

			assertThat(cellOf(nox, "2026-M02").status()).isEqualTo(FulfillmentStatus.FULFILLED);
			assertThat(cellOf(nox, "2026-M01").status()).isEqualTo(FulfillmentStatus.OVERDUE);
			assertThat(cellOf(nox, "2026-M03").status()).isEqualTo(FulfillmentStatus.PENDING);
			assertThat(cellOf(nox, "2026-M12").status()).isEqualTo(FulfillmentStatus.PENDING);
		}

		@Test
		@DisplayName("셀은 그 구간을 이행한 회차를 가리켜 화면 이동을 가능하게 한다")
		void exposesScheduleIds() {
			stackQuery.items.add(itemOf(11L, MeasurementCycle.QUARTERLY));
			givenRecord(7L, 11L, MeasurementCycle.QUARTERLY, LocalDate.of(2026, 2, 10));

			FulfillmentBoardDetail.Cell q1 = cellOf(assemble().rows().getFirst(), "2026-Q1");

			assertThat(q1.scheduleIds()).containsExactly(7L);
		}

		@Test
		@DisplayName("월 2회 주기는 한 번만 측정하면 부분 이행이다")
		void marksPartialForTwiceMonthly() {
			stackQuery.items.add(itemOf(11L, MeasurementCycle.TWICE_MONTHLY));
			givenRecord(1L, 11L, MeasurementCycle.TWICE_MONTHLY, LocalDate.of(2026, 3, 5));

			FulfillmentBoardDetail.Cell march = cellOf(assemble().rows().getFirst(), "2026-T03");

			assertThat(march.fulfilledCount()).isEqualTo(1);
			assertThat(march.requiredCount()).isEqualTo(2);
			assertThat(march.status()).isEqualTo(FulfillmentStatus.PARTIAL);
		}

		@Test
		@DisplayName("월 2회 주기는 같은 달 두 회차를 채우면 이행이다")
		void marksFulfilledOnSecondRecord() {
			stackQuery.items.add(itemOf(11L, MeasurementCycle.TWICE_MONTHLY));
			givenRecord(1L, 11L, MeasurementCycle.TWICE_MONTHLY, LocalDate.of(2026, 3, 5));
			givenRecord(2L, 11L, MeasurementCycle.TWICE_MONTHLY, LocalDate.of(2026, 3, 20));

			FulfillmentBoardDetail.Cell march = cellOf(assemble().rows().getFirst(), "2026-T03");

			assertThat(march.status()).isEqualTo(FulfillmentStatus.FULFILLED);
			// 이력이 측정일 내림차순이라 최근 회차가 앞에 온다. 셀이 가리키는 회차 집합만 계약이고 순서는 아니다.
			assertThat(march.scheduleIds()).containsExactlyInAnyOrder(1L, 2L);
		}

		@Test
		@DisplayName("주기를 바꾸면 옛 주기의 기록이 새 주기 칸을 채우지 않는다")
		void doesNotMatchRecordsOfAnotherCycle() {
			// 원장은 분기로 바뀌었지만 과거 기록은 월 주기로 남아 있다
			stackQuery.items.add(itemOf(11L, MeasurementCycle.QUARTERLY));
			givenRecord(1L, 11L, MeasurementCycle.MONTHLY, LocalDate.of(2026, 2, 10));

			FulfillmentBoardDetail.Cell q1 = cellOf(assemble().rows().getFirst(), "2026-Q1");

			assertThat(q1.fulfilledCount()).isZero();
		}

		@Test
		@DisplayName("다른 항목의 기록이 섞이지 않는다")
		void separatesItems() {
			stackQuery.items.add(itemOf(11L, MeasurementCycle.QUARTERLY));
			stackQuery.items.add(itemOf(22L, MeasurementCycle.QUARTERLY));
			givenRecord(1L, 11L, MeasurementCycle.QUARTERLY, LocalDate.of(2026, 2, 10));

			FulfillmentBoardDetail board = assemble();

			assertThat(cellOf(board.rows().get(0), "2026-Q1").fulfilledCount()).isEqualTo(1);
			assertThat(cellOf(board.rows().get(1), "2026-Q1").fulfilledCount()).isZero();
		}
	}

	@Nested
	@DisplayName("조회 횟수 — 항목 수와 무관하게 고정이다")
	class QueryCount {

		@Test
		@DisplayName("항목이 늘어도 원장을 한 번만 읽는다")
		void keepsSingleLedgerQuery() {
			for (long id = 1; id <= 20; id++) {
				stackQuery.items.add(itemOf(id, MeasurementCycle.MONTHLY));
			}

			assemble();

			assertThat(stackQuery.callCount).isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("미이행 목록 — 조치가 필요한 셀만 펼친다")
	class Pending {

		@Test
		@DisplayName("기한이 지난 구간이 목록 앞에 오고 남은 일수는 음수다")
		void putsOverdueFirst() {
			stackQuery.items.add(itemOf(11L, MeasurementCycle.MONTHLY));

			List<PendingMeasurementListItem> pending =
				assembler.assemblePending(TENANT_ID, YEAR, TODAY, 30);

			assertThat(pending).isNotEmpty();
			assertThat(pending.getFirst().periodKey()).isEqualTo("2026-M01");
			assertThat(pending.getFirst().daysRemaining()).isNegative();
			assertThat(pending).isSortedAccordingTo(
				Comparator.comparingLong(PendingMeasurementListItem::daysRemaining));
		}

		@Test
		@DisplayName("이행한 구간은 목록에 넣지 않는다")
		void excludesFulfilled() {
			stackQuery.items.add(itemOf(11L, MeasurementCycle.MONTHLY));
			givenRecord(1L, 11L, MeasurementCycle.MONTHLY, LocalDate.of(2026, 1, 10));

			List<PendingMeasurementListItem> pending =
				assembler.assemblePending(TENANT_ID, YEAR, TODAY, 30);

			assertThat(pending).noneMatch(item -> item.periodKey().equals("2026-M01"));
		}

		@Test
		@DisplayName("지정한 일수보다 먼 미래 구간은 아직 알리지 않는다")
		void excludesFarFuture() {
			stackQuery.items.add(itemOf(11L, MeasurementCycle.MONTHLY));

			List<PendingMeasurementListItem> pending =
				assembler.assemblePending(TENANT_ID, YEAR, TODAY, 30);

			// 3월 15일 기준 30일 안에 기한이 닿는 것은 3월(3/31)까지다
			assertThat(pending).anyMatch(item -> item.periodKey().equals("2026-M03"));
			assertThat(pending).noneMatch(item -> item.periodKey().equals("2026-M05"));
		}
	}

	/** 원장 조회 횟수를 셀 수 있는 최소 구현. */
	private static class FakeStackQueryUseCase implements StackQueryUseCase {

		private final List<StackMeasurementItemSummary> items = new ArrayList<>();
		private int callCount = 0;

		@Override
		public StackMeasurementSummary getMeasurementTargetSummary(Long stackId, Long tenantId) {
			throw new UnsupportedOperationException("현황판 조립은 이 경로를 쓰지 않는다");
		}

		@Override
		public long countStacks(Long tenantId) {
			return 1;
		}

		@Override
		public List<StackMeasurementItemSummary> findMeasurementItems(
			Long tenantId, Long workplaceId, Long stackId) {
			callCount++;
			return List.copyOf(items);
		}
	}
}
