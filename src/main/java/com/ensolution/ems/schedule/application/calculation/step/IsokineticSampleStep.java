package com.ensolution.ems.schedule.application.calculation.step;

import com.ensolution.ems.schedule.application.calculation.Calculator;
import com.ensolution.ems.schedule.application.calculation.SheetContext;
import com.ensolution.ems.schedule.domain.sampling.GaseousSampling;
import com.ensolution.ems.schedule.domain.sampling.ParticulateSampling;
import com.ensolution.ems.schedule.domain.sampling.SamplingSheet;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 등속흡인 트레인에 담기는 가스상 시료 행을 입자상 집계로 채운다.
 *
 * <p>비소화합물처럼 등속흡인 트레인의 임핀저(흡수액)에 담기는 시료는 흡인유량이 계획값이 아니다 —
 * 입자상 채취가 측정점마다 오리피스 차압으로 유량을 조절하므로, 그 행의 채취량은 건식가스미터 채취량(Vm)이고
 * 유량은 {@code Vm / 총채취시간}이다. 사용자가 손으로 옮겨 적으면 입자상 값을 고칠 때마다 어긋나므로
 * <b>서버가 저장할 때마다 덮어쓴다</b>. 클라이언트가 무엇을 보내든 이 값이 확정값이며, 출처가 비면 파생값도
 * 비운다(옛 값이 남으면 안 된다). 시각·유량·채취량 이외의 칸(시료번호·가스미터압·온도·적산값·항목)은 입력값이라
 * 손대지 않는다.
 *
 * <p>어느 행이 등속흡인 행인지는 {@link SheetContext#particulateSourceOf(List)}가 정한다 — 카탈로그
 * {@code MeasurementMode}가 등속흡인 방식(먼지·중금속·수은)인 항목({@code SamplingItemInput.particulateSource})이
 * 하나라도 담긴 병은 등속흡인 트레인의 병이다. <b>그런 행은 그 방식의 입자상 기록지에만 적힌다</b>
 * ({@code ScheduleValidator#requireIsokineticRowsOnSourceSheet}) — 비소는 중금속 기록지에만 있다. 그래서 출처는
 * 언제나 같은 기록지의 {@code particulateSampling}이며 기록지 밖을 보지 않는다. 검증을 우회해 다른 기록지에 놓인
 * 행은 출처가 없으므로 손대지 않는다.
 *
 * <p>단위: 입자상 Vm은 m³·총채취시간은 분, 가스상 행의 채취량은 L·흡인유량은 L/min이다.
 * {@link ParticleStep} 다음에 돌아야 이번 저장의 집계값을 본다.
 */
@Component
@Order(9)
@RequiredArgsConstructor
public class IsokineticSampleStep implements SheetStep {

	private static final BigDecimal LITERS_PER_CUBIC_METER = BigDecimal.valueOf(1000);
	private static final int SCALE = 2;
	private static final int DIVIDE_SCALE = 10;

	private final Calculator calculator;

	@Override
	public void execute(SheetContext context) {
		SamplingSheet sheet = context.getSheet();
		List<GaseousSampling> rows = sheet.getGaseousSamplings();
		ParticulateSampling particle = sheet.getParticulateSampling();
		if (rows == null || rows.isEmpty() || particle == null) return;

		List<GaseousSampling> projected = rows.stream()
			.map(row -> isSourcedHere(row, context) ? project(row, particle) : row)
			.toList();

		context.setSheet(sheet.toBuilder().gaseousSamplings(projected).build());
	}

	/** 등속흡인 행이면서 이 기록지가 그 방식의 입자상 기록지인가. */
	private static boolean isSourcedHere(GaseousSampling row, SheetContext context) {
		return context.particulateSourceOf(row.getPollutantIds())
			.map(source -> source == context.getSheet().getCategory())
			.orElse(false);
	}

	private GaseousSampling project(GaseousSampling row, ParticulateSampling particle) {
		BigDecimal volumeLiters = toLiters(particle.getTotalDryGasVolume());
		return row.toBuilder()
			.samplingStartedAt(particle.getSamplingStartedAt())
			.samplingEndedAt(particle.getSamplingEndedAt())
			.samplingVolume(calculator.round(volumeLiters, SCALE))
			.suctionQuantity(calculator.round(flowRate(volumeLiters, particle.getTotalSamplingTime()), SCALE))
			.build();
	}

	// 채취량(L) = Vm(m³) × 1000
	private static BigDecimal toLiters(BigDecimal cubicMeters) {
		return cubicMeters == null ? null : cubicMeters.multiply(LITERS_PER_CUBIC_METER);
	}

	// 흡인유량(L/min) = 채취량(L) / 총채취시간(min). 시간이 없거나 0이면 정의되지 않는다.
	private static BigDecimal flowRate(BigDecimal liters, BigDecimal minutes) {
		if (liters == null || minutes == null || minutes.signum() == 0) return null;
		return liters.divide(minutes, DIVIDE_SCALE, RoundingMode.HALF_UP);
	}
}
