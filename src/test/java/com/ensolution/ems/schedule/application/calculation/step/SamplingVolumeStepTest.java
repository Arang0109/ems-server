package com.ensolution.ems.schedule.application.calculation.step;

import com.ensolution.ems.schedule.application.calculation.Calculator;
import com.ensolution.ems.schedule.application.calculation.SamplingItemInput;
import com.ensolution.ems.schedule.application.calculation.SheetContext;
import com.ensolution.ems.schedule.domain.sampling.GaseousSampling;
import com.ensolution.ems.schedule.domain.sampling.MeasurementCategory;
import com.ensolution.ems.schedule.domain.sampling.SamplingSheet;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 비어 있는 시료채취량을 채취시간 × 흡인유량으로 채우되, 적힌 값·등속흡인 행·입력이 모자란 행은 건드리지 않음을
 * 고정한다 — 기본값이지 확정값이 아니다.
 */
class SamplingVolumeStepTest {

	private static final Long SO2 = 32L;
	private static final Long ARSENIC = 31L;
	private static final List<SamplingItemInput> ITEMS = List.of(
		new SamplingItemInput(SO2, null, 30),
		new SamplingItemInput(ARSENIC, MeasurementCategory.HEAVY_METAL, null));

	private final SamplingVolumeStep step = new SamplingVolumeStep(new Calculator());

	private static GaseousSampling row(LocalTime start, LocalTime end, String flowRate, String volume, Long... ids) {
		return GaseousSampling.builder()
			.samplingStartedAt(start)
			.samplingEndedAt(end)
			.suctionQuantity(flowRate == null ? null : new BigDecimal(flowRate))
			.samplingVolume(volume == null ? null : new BigDecimal(volume))
			.pollutantIds(ids.length == 0 ? null : List.of(ids))
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
	void 비어_있는_채취량을_채취시간_곱하기_흡인유량으로_채운다() {
		GaseousSampling result = run(row(LocalTime.of(9, 10), LocalTime.of(9, 40), "1.5", null, SO2));

		assertThat(result.getSamplingVolume()).isEqualByComparingTo("45.0");
	}

	@Test
	void 소수_첫째_자리로_반올림한다() {
		// 47분 × 0.23 = 10.81 → 10.8
		GaseousSampling result = run(row(LocalTime.of(9, 0), LocalTime.of(9, 47), "0.23", null, SO2));

		assertThat(result.getSamplingVolume()).isEqualByComparingTo("10.8");
	}

	@Test
	void 자정을_넘긴_채취는_하루를_더해_센다() {
		GaseousSampling result = run(row(LocalTime.of(23, 50), LocalTime.of(0, 20), "1", null, SO2));

		assertThat(result.getSamplingVolume()).isEqualByComparingTo("30.0");
	}

	@Test
	void 적힌_채취량은_손대지_않는다() {
		GaseousSampling result = run(row(LocalTime.of(9, 10), LocalTime.of(9, 40), "1.5", "44.2", SO2));

		assertThat(result.getSamplingVolume()).isEqualByComparingTo("44.2");
	}

	@Test
	void 시각이나_유량이_없으면_채우지_않는다() {
		assertThat(run(row(LocalTime.of(9, 10), null, "1.5", null, SO2)).getSamplingVolume()).isNull();
		assertThat(run(row(LocalTime.of(9, 10), LocalTime.of(9, 40), null, null, SO2)).getSamplingVolume()).isNull();
	}

	@Test
	void 시작과_종료가_같으면_채취시간이_없어_채우지_않는다() {
		assertThat(run(row(LocalTime.of(9, 10), LocalTime.of(9, 10), "1.5", null, SO2)).getSamplingVolume()).isNull();
	}

	@Test
	void 등속흡인_행은_채취량이_입자상_사본이라_건너뛴다() {
		GaseousSampling result = run(row(LocalTime.of(9, 10), LocalTime.of(9, 40), "1.5", null, ARSENIC));

		assertThat(result.getSamplingVolume()).isNull();
	}

	@Test
	void 항목_링크가_없는_수동_행도_시각과_유량이_있으면_채운다() {
		GaseousSampling result = run(row(LocalTime.of(9, 10), LocalTime.of(9, 40), "2", null));

		assertThat(result.getSamplingVolume()).isEqualByComparingTo("60.0");
	}
}
