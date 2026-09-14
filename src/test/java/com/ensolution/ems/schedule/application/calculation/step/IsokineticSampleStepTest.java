package com.ensolution.ems.schedule.application.calculation.step;

import com.ensolution.ems.schedule.application.calculation.Calculator;
import com.ensolution.ems.schedule.application.calculation.SamplingItemInput;
import com.ensolution.ems.schedule.application.calculation.SheetContext;
import com.ensolution.ems.schedule.domain.sampling.GaseousSampling;
import com.ensolution.ems.schedule.domain.sampling.MeasurementCategory;
import com.ensolution.ems.schedule.domain.sampling.ParticulateSampling;
import com.ensolution.ems.schedule.domain.sampling.SamplingSheet;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 등속흡인 트레인에 담기는 가스상 시료 행(비소화합물 흡수액)은 채취시각·흡인유량·채취량이 입력값이 아니라
 * 같은 시트 입자상 집계의 파생값임을 고정한다 — 클라이언트가 무엇을 보내든 서버가 덮어쓰고, 출처가 비면
 * 파생값도 비우며, 정유량 행과 나머지 칸은 손대지 않는다.
 */
class IsokineticSampleStepTest {

	private static final Long ARSENIC = 31L;   // HEAVY_METAL — 등속흡인
	private static final Long SO2 = 32L;       // GAS_SAMPLING — 정유량
	private static final List<SamplingItemInput> ITEMS = List.of(
		new SamplingItemInput(ARSENIC, MeasurementCategory.HEAVY_METAL, null),
		new SamplingItemInput(SO2, null, 30));

	private final IsokineticSampleStep step = new IsokineticSampleStep(new Calculator());

	/** 클라이언트가 임의로 채워 보낸 행. 입력 칸(시료번호·가스미터압)은 유지되어야 한다. */
	private static GaseousSampling row(Long... pollutantIds) {
		return GaseousSampling.builder()
			.sampleName("비소")
			.samplingStartedAt(LocalTime.of(9, 0))
			.samplingEndedAt(LocalTime.of(9, 30))
			.suctionQuantity(new BigDecimal("99"))
			.samplingVolume(new BigDecimal("999"))
			.gasMeterGaugePressure(new BigDecimal("12"))
			.sampleNumber("S-1")
			.pollutantIds(pollutantIds.length == 0 ? null : List.of(pollutantIds))
			.build();
	}

	/** Vm 0.12 m³ 를 60분 동안 — 채취량 120 L, 유량 2 L/min. */
	private static ParticulateSampling particle() {
		return ParticulateSampling.builder()
			.samplingStartedAt(LocalTime.of(10, 0))
			.samplingEndedAt(LocalTime.of(11, 0))
			.totalDryGasVolume(new BigDecimal("0.12000"))
			.totalSamplingTime(new BigDecimal("60.0"))
			.build();
	}

	private static SheetContext context(ParticulateSampling particle, List<SamplingItemInput> items, GaseousSampling... rows) {
		return context(MeasurementCategory.HEAVY_METAL, particle, items, rows);
	}

	private static SheetContext context(MeasurementCategory category, ParticulateSampling particle,
	                                    List<SamplingItemInput> items, GaseousSampling... rows) {
		SamplingSheet sheet = SamplingSheet.builder()
			.category(category)
			.particulateSampling(particle)
			.gaseousSamplings(List.of(rows))
			.build();
		return new SheetContext(sheet, null, null, null, null, items);
	}

	private GaseousSampling first(SheetContext context) {
		step.execute(context);
		return context.getSheet().getGaseousSamplings().getFirst();
	}

	@Test
	void 등속흡인_항목이_담긴_행은_입자상_집계로_시각_유량_채취량을_덮어쓴다() {
		GaseousSampling result = first(context(particle(), ITEMS, row(ARSENIC)));

		assertThat(result.getSamplingStartedAt()).isEqualTo(LocalTime.of(10, 0));
		assertThat(result.getSamplingEndedAt()).isEqualTo(LocalTime.of(11, 0));
		assertThat(result.getSamplingVolume()).isEqualByComparingTo("120.00");
		assertThat(result.getSuctionQuantity()).isEqualByComparingTo("2.00");
	}

	@Test
	void 나머지_칸은_입력값이라_손대지_않는다() {
		GaseousSampling result = first(context(particle(), ITEMS, row(ARSENIC)));

		assertThat(result.getSampleName()).isEqualTo("비소");
		assertThat(result.getSampleNumber()).isEqualTo("S-1");
		assertThat(result.getGasMeterGaugePressure()).isEqualByComparingTo("12");
		assertThat(result.getPollutantIds()).containsExactly(ARSENIC);
	}

	@Test
	void 정유량_항목만_담긴_행은_손대지_않는다() {
		GaseousSampling result = first(context(particle(), ITEMS, row(SO2)));

		assertThat(result.getSuctionQuantity()).isEqualByComparingTo("99");
		assertThat(result.getSamplingVolume()).isEqualByComparingTo("999");
		assertThat(result.getSamplingStartedAt()).isEqualTo(LocalTime.of(9, 0));
	}

	@Test
	void 등속흡인_항목과_정유량_항목이_섞인_행도_등속흡인으로_본다() {
		GaseousSampling result = first(context(particle(), ITEMS, row(SO2, ARSENIC)));

		assertThat(result.getSamplingVolume()).isEqualByComparingTo("120.00");
	}

	@Test
	void pollutantIds가_없는_행은_손대지_않는다() {
		GaseousSampling result = first(context(particle(), ITEMS, row()));

		assertThat(result.getSuctionQuantity()).isEqualByComparingTo("99");
	}

	@Test
	void 입자상_집계가_없으면_행을_그대로_둔다() {
		GaseousSampling result = first(context(null, ITEMS, row(ARSENIC)));

		assertThat(result.getSuctionQuantity()).isEqualByComparingTo("99");
		assertThat(result.getSamplingVolume()).isEqualByComparingTo("999");
	}

	@Test
	void 총채취시간이_0이면_유량은_null이고_채취량은_남는다() {
		ParticulateSampling zeroTime = particle().toBuilder().totalSamplingTime(BigDecimal.ZERO).build();

		GaseousSampling result = first(context(zeroTime, ITEMS, row(ARSENIC)));

		assertThat(result.getSuctionQuantity()).isNull();
		assertThat(result.getSamplingVolume()).isEqualByComparingTo("120.00");
	}

	@Test
	void 입자상_값이_비면_파생값도_null로_덮어쓴다() {
		ParticulateSampling empty = ParticulateSampling.builder().build();

		GaseousSampling result = first(context(empty, ITEMS, row(ARSENIC)));

		assertThat(result.getSamplingStartedAt()).isNull();
		assertThat(result.getSamplingEndedAt()).isNull();
		assertThat(result.getSuctionQuantity()).isNull();
		assertThat(result.getSamplingVolume()).isNull();
	}

	@Test
	void 출처_기록지가_아닌_곳에_놓인_등속흡인_행은_손대지_않는다() {
		// 검증(requireIsokineticRowsOnSourceSheet)이 막는 배치지만, 우회되더라도 남의 집계로 채우지는 않는다.
		GaseousSampling result = first(context(MeasurementCategory.DUST, particle(), ITEMS, row(ARSENIC)));

		assertThat(result.getSuctionQuantity()).isEqualByComparingTo("99");
		assertThat(result.getSamplingVolume()).isEqualByComparingTo("999");
	}

	@Test
	void 항목_입력이_없으면_아무것도_하지_않는다() {
		GaseousSampling result = first(context(particle(), List.of(), row(ARSENIC)));

		assertThat(result.getSuctionQuantity()).isEqualByComparingTo("99");
	}

	@Test
	void 유량은_소수_둘째_자리로_반올림한다() {
		// 0.1 m³ / 60분 = 1.6666… L/min
		ParticulateSampling p = particle().toBuilder().totalDryGasVolume(new BigDecimal("0.1")).build();

		GaseousSampling result = first(context(p, ITEMS, row(ARSENIC)));

		assertThat(result.getSuctionQuantity()).isEqualByComparingTo("1.67");
		assertThat(result.getSamplingVolume()).isEqualByComparingTo("100.00");
	}
}
