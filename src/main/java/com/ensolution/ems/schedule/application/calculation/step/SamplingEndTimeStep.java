package com.ensolution.ems.schedule.application.calculation.step;

import com.ensolution.ems.schedule.application.calculation.SheetContext;
import com.ensolution.ems.schedule.domain.sampling.GaseousSampling;
import com.ensolution.ems.schedule.domain.sampling.SamplingSheet;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 시작시각만 적힌 가스상 시료 행의 종료시각을 <b>시작시각 + 표준 채취시간</b>으로 채운다.
 *
 * <p>정유량 채취는 계획한 시간만큼 흡인하므로 종료시각은 대개 시작시각에서 결정된다. 다만 현장 사정으로 일찍 끝내거나
 * 늦게 끝내는 일이 있어 <b>기본값</b>이지 확정값이 아니다 — 종료시각이 이미 적혀 있으면 손대지 않는다. 그래서
 * 시작시각을 고친 뒤의 종료시각 재계산은 화면의 몫이고, 여기서는 비어 있는 칸만 메운다.
 *
 * <p>등속흡인 행({@link IsokineticSampleStep})은 시각이 입자상 집계의 사본이므로 건너뛴다 — 입자상 종료시각이
 * 비어 있다면 그 행도 비어 있어야지 채취시간으로 지어내면 안 된다. 표준 채취시간이 정해지지 않은 항목도 건너뛴다.
 */
@Component
@Order(10)
public class SamplingEndTimeStep implements SheetStep {

	@Override
	public void execute(SheetContext context) {
		SamplingSheet sheet = context.getSheet();
		List<GaseousSampling> rows = sheet.getGaseousSamplings();
		if (rows == null || rows.isEmpty()) return;

		List<GaseousSampling> filled = rows.stream()
			.map(row -> fillEndTime(row, context))
			.toList();

		context.setSheet(sheet.toBuilder().gaseousSamplings(filled).build());
	}

	private static GaseousSampling fillEndTime(GaseousSampling row, SheetContext context) {
		if (row.getSamplingStartedAt() == null || row.getSamplingEndedAt() != null) return row;
		if (context.containsIsokineticItem(row.getPollutantIds())) return row;

		return context.samplingMinutesOf(row.getPollutantIds())
			.map(minutes -> row.toBuilder().samplingEndedAt(row.getSamplingStartedAt().plusMinutes(minutes)).build())
			.orElse(row);
	}
}
