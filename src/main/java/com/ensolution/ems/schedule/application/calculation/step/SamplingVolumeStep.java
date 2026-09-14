package com.ensolution.ems.schedule.application.calculation.step;

import com.ensolution.ems.schedule.application.calculation.Calculator;
import com.ensolution.ems.schedule.application.calculation.SheetContext;
import com.ensolution.ems.schedule.domain.sampling.GaseousSampling;
import com.ensolution.ems.schedule.domain.sampling.SamplingSheet;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

/**
 * 시료채취량이 비어 있는 정유량 행을 <b>채취시간(분) × 흡인유량(L/min)</b>으로 채운다.
 *
 * <p>정유량 펌프는 정해 둔 유량으로 정해진 시간만큼 흡인하므로 채취량은 둘의 곱이다. 현장에서 적산계를 읽어
 * 적는 값이 더 정확할 수 있으므로 <b>비어 있을 때만</b> 채우는 기본값이며, 적힌 값은 손대지 않는다 —
 * {@link SamplingEndTimeStep}과 같은 규약이다. 시각·유량이 바뀔 때 즉시 다시 계산하는 것은 화면
 * ({@code ems-web} {@code sample-rules.applySamplePatch})의 몫이다.
 *
 * <p>등속흡인 행({@link IsokineticSampleStep})은 채취량이 입자상 Vm의 사본이라 건너뛴다. 종료시각이 시작보다 앞이면
 * 자정을 넘긴 것으로 보아 하루를 더한다(기록지는 날짜 없이 시각만 적는다). 둘이 같으면 채취시간이 없어 채우지 않는다.
 * {@link SamplingEndTimeStep} 다음에 돌아야 방금 채운 종료시각을 본다.
 */
@Component
@Order(11)
@RequiredArgsConstructor
public class SamplingVolumeStep implements SheetStep {

	/** 소수 자릿수 — 프론트 입력칸(samplingVolume, 소수 1자리)·화면 계산과 같다 */
	private static final int SCALE = 1;
	private static final long MINUTES_PER_DAY = 24 * 60;

	private final Calculator calculator;

	@Override
	public void execute(SheetContext context) {
		SamplingSheet sheet = context.getSheet();
		List<GaseousSampling> rows = sheet.getGaseousSamplings();
		if (rows == null || rows.isEmpty()) return;

		List<GaseousSampling> filled = rows.stream()
			.map(row -> fillVolume(row, context))
			.toList();

		context.setSheet(sheet.toBuilder().gaseousSamplings(filled).build());
	}

	private GaseousSampling fillVolume(GaseousSampling row, SheetContext context) {
		if (row.getSamplingVolume() != null || row.getSuctionQuantity() == null) return row;
		if (context.containsIsokineticItem(row.getPollutantIds())) return row;

		BigDecimal minutes = durationMinutes(row.getSamplingStartedAt(), row.getSamplingEndedAt());
		if (minutes == null) return row;

		return row.toBuilder()
			.samplingVolume(calculator.round(minutes.multiply(row.getSuctionQuantity()), SCALE))
			.build();
	}

	/** 채취시간(분) = 종료 − 시작. 자정을 넘기면 하루를 더한다. 비었거나 0분이면 null */
	private static BigDecimal durationMinutes(LocalTime start, LocalTime end) {
		if (start == null || end == null) return null;
		long minutes = Math.floorMod(Duration.between(start, end).toMinutes(), MINUTES_PER_DAY);
		return minutes == 0 ? null : BigDecimal.valueOf(minutes);
	}
}
