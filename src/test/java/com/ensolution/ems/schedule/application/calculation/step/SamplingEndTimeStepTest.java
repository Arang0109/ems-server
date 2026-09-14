package com.ensolution.ems.schedule.application.calculation.step;

import com.ensolution.ems.schedule.application.calculation.SamplingItemInput;
import com.ensolution.ems.schedule.application.calculation.SheetContext;
import com.ensolution.ems.schedule.domain.sampling.GaseousSampling;
import com.ensolution.ems.schedule.domain.sampling.MeasurementCategory;
import com.ensolution.ems.schedule.domain.sampling.SamplingSheet;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 시작시각만 적힌 가스상 시료 행의 종료시각을 표준 채취시간으로 채우되, 이미 적힌 종료시각·등속흡인 행·
 * 채취시간이 없는 항목은 건드리지 않음을 고정한다 — 기본값이지 확정값이 아니다.
 */
class SamplingEndTimeStepTest {

	private static final Long SO2 = 32L;       // 흡수액 30분
	private static final Long NOX_BAG = 33L;   // 채취시간 미지정
	private static final Long ARSENIC = 31L;   // 등속흡인
	private static final List<SamplingItemInput> ITEMS = List.of(
		new SamplingItemInput(SO2, null, 30),
		new SamplingItemInput(NOX_BAG, null, null),
		new SamplingItemInput(ARSENIC, MeasurementCategory.HEAVY_METAL, 60));

	private final SamplingEndTimeStep step = new SamplingEndTimeStep();

	private static GaseousSampling row(LocalTime start, LocalTime end, Long... pollutantIds) {
		return GaseousSampling.builder()
			.samplingStartedAt(start)
			.samplingEndedAt(end)
			.pollutantIds(pollutantIds.length == 0 ? null : List.of(pollutantIds))
			.build();
	}

	private GaseousSampling run(GaseousSampling row) {
		SamplingSheet sheet = SamplingSheet.builder()
			.category(MeasurementCategory.GAS)
			.gaseousSamplings(List.of(row))
			.build();
		SheetContext context = new SheetContext(sheet, null, null, null, null, ITEMS);
		step.execute(context);
		return context.getSheet().getGaseousSamplings().getFirst();
	}

	@Test
	void 시작시각만_있으면_표준_채취시간을_더해_종료시각을_채운다() {
		GaseousSampling result = run(row(LocalTime.of(9, 10), null, SO2));

		assertThat(result.getSamplingEndedAt()).isEqualTo(LocalTime.of(9, 40));
	}

	@Test
	void 종료시각이_이미_있으면_손대지_않는다() {
		GaseousSampling result = run(row(LocalTime.of(9, 10), LocalTime.of(9, 25), SO2));

		assertThat(result.getSamplingEndedAt()).isEqualTo(LocalTime.of(9, 25));
	}

	@Test
	void 시작시각이_없으면_종료시각도_채우지_않는다() {
		GaseousSampling result = run(row(null, null, SO2));

		assertThat(result.getSamplingEndedAt()).isNull();
	}

	@Test
	void 채취시간이_정해지지_않은_항목은_건너뛴다() {
		GaseousSampling result = run(row(LocalTime.of(9, 10), null, NOX_BAG));

		assertThat(result.getSamplingEndedAt()).isNull();
	}

	@Test
	void 등속흡인_행은_입자상_시각의_사본이라_건너뛴다() {
		GaseousSampling result = run(row(LocalTime.of(9, 10), null, ARSENIC));

		assertThat(result.getSamplingEndedAt()).isNull();
	}

	@Test
	void 항목이_없는_행은_건너뛴다() {
		GaseousSampling result = run(row(LocalTime.of(9, 10), null));

		assertThat(result.getSamplingEndedAt()).isNull();
	}

	@Test
	void 한_병에_담긴_여러_항목은_처음_만나는_채취시간을_쓴다() {
		GaseousSampling result = run(row(LocalTime.of(23, 50), null, NOX_BAG, SO2));

		// 자정을 넘기면 LocalTime 이 감아 돈다 — 날짜 없이 시각만 적는 기록지 규약과 같다.
		assertThat(result.getSamplingEndedAt()).isEqualTo(LocalTime.of(0, 20));
	}
}
