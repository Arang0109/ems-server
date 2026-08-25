package com.ensolution.ems.schedule.domain.snapshot;

import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 측정항목 재배치 검증. 이 순서가 곧 성적서의 항목 배치이므로
 * (템플릿이 {@code items[0]}~{@code items[3]} 처럼 인덱스로 칸을 지목한다) 순서와 내용이 정확해야 한다.
 */
class ScheduleSnapshotItemOrderTest {

	private SamplingItemSnapshot item(Long pollutantId, String nameKr) {
		return new SamplingItemSnapshot(pollutantId * 10, pollutantId, null, nameKr, null,
			null, null, null, null, null, null, null, false);
	}

	private ScheduleSnapshot snapshot(SamplingItemSnapshot... items) {
		return new ScheduleSnapshot("1", 1L, 1L, null, null, null, null, null, null,
			items == null ? null : Arrays.asList(items), null, null, null);
	}

	@Test
	void 요청_순서대로_항목이_재배치된다() {
		ScheduleSnapshot snapshot = snapshot(item(1L, "먼지"), item(2L, "질소산화물"), item(3L, "황산화물"));

		ScheduleSnapshot reordered = snapshot.withItemOrder(List.of(3L, 1L, 2L));

		assertThat(reordered.items())
			.extracting(SamplingItemSnapshot::nameKr)
			.containsExactly("황산화물", "먼지", "질소산화물");
	}

	@Test
	void 항목의_내용은_그대로_유지된다() {
		SamplingItemSnapshot dust = item(1L, "먼지");
		ScheduleSnapshot snapshot = snapshot(dust, item(2L, "질소산화물"));

		ScheduleSnapshot reordered = snapshot.withItemOrder(List.of(2L, 1L));

		// 재배치는 자리만 바꾼다 — 같은 인스턴스가 그대로 옮겨간다
		assertThat(reordered.items().getLast()).isSameAs(dust);
	}

	@Test
	void 항목_외의_스냅샷은_건드리지_않는다() {
		List<MeasurementSheet> sheets = List.of(MeasurementSheet.builder().build());
		ScheduleSnapshot snapshot = new ScheduleSnapshot("1", 5L, 7L, null, null, null, null, null,
			List.of(), List.of(item(1L, "먼지"), item(2L, "질소산화물")), sheets, 3L, null);

		ScheduleSnapshot reordered = snapshot.withItemOrder(List.of(2L, 1L));

		assertThat(reordered.scheduleId()).isEqualTo(5L);
		assertThat(reordered.tenantId()).isEqualTo(7L);
		assertThat(reordered.version()).isEqualTo(3L);
		assertThat(reordered.sheets()).isSameAs(snapshot.sheets());   // 계산 입력이 아니므로 시트는 그대로
	}

	@Test
	void 항목이_하나면_순서가_그대로다() {
		ScheduleSnapshot reordered = snapshot(item(1L, "먼지")).withItemOrder(List.of(1L));

		assertThat(reordered.items()).extracting(SamplingItemSnapshot::nameKr).containsExactly("먼지");
	}

	@Test
	void 항목이_없는_스냅샷도_깨지지_않는다() {
		// 집합 일치 검증을 통과하지 못할 입력이지만, 도메인이 NPE로 터지지는 않아야 한다
		assertThat(snapshot((SamplingItemSnapshot[]) null).withItemOrder(List.of(1L)).items()).isEmpty();
	}
}
