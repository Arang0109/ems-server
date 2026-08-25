package com.ensolution.ems.schedule.domain.history;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.snapshot.SamplingItemSnapshot;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 한 회차에 한 측정항목을 측정했다는 확정 기록. 측정계획이 완료될 때 항목 수만큼 만들어지고,
 * 재개방·취소되면 그 계획의 기록이 통째로 사라진다(이행 해제).
 *
 * <p>측정물질명·주기·허용기준을 <b>행에 복사해 둔다.</b> 원장({@code stack_pollutant})이 나중에 바뀌어도
 * 과거의 이행 판정과 초과 판정이 흔들리면 안 되기 때문이며, {@link SamplingItemSnapshot}이 이미 같은
 * 이유로 측정 시점 값을 들고 있다. 그래서 기록을 만들 때 원장을 다시 읽지 않고 스냅샷만 본다.
 *
 * @param periodYear  구간 연도. 주기가 없는 항목도 연도별 조회에 걸려야 하므로 항상 측정일의 연도로 채운다
 * @param periodIndex 구간 번호. 주기가 지정되지 않은 항목은 구간을 특정할 수 없으므로 null이다
 * @param result      확정 결과값. 분석 결과가 아직 없으면 {@link MeasurementResult#empty()}
 */
public record MeasurementRecord(
	Long id,
	Long tenantId,
	Long stackId,
	Long scheduleId,
	Long stackPollutantId,
	Long pollutantId,
	String pollutantCode,
	String pollutantNameKr,
	MeasurementCycle cycle,
	int periodYear,
	Integer periodIndex,
	LocalDate sampledAt,
	LocalDateTime completedAt,
	MeasurementResult result
) {

	/**
	 * 완료된 측정계획의 측정항목 하나를 기록으로 만든다.
	 * 구간은 측정일(채취일)로 정한다 — 완료 처리가 늦어져도 이행한 구간은 측정한 때가 결정한다.
	 */
	public static MeasurementRecord ofCompletion(
		Schedule completed, SamplingItemSnapshot item, MeasurementResult result) {

		MeasurementPeriod period = MeasurementPeriod.of(item.cycle(), completed.getSampledAt());

		return new MeasurementRecord(
			null,
			completed.getTenantId(),
			completed.getStackId(),
			completed.getId(),
			item.stackPollutantId(),
			item.pollutantId(),
			item.code(),
			item.nameKr(),
			item.cycle(),
			completed.getSampledAt().getYear(),
			period == null ? null : period.index(),
			completed.getSampledAt(),
			LocalDateTime.now(),
			result == null ? MeasurementResult.empty() : result
		);
	}

	/** 이 기록이 이행한 주기 구간. 주기가 지정되지 않은 항목은 null이다. */
	public MeasurementPeriod period() {
		if (cycle == null || periodIndex == null) return null;
		return new MeasurementPeriod(cycle, periodYear, periodIndex);
	}

	/** 주기 이행 집계 대상인지 여부. 주기가 없는 항목은 이력으로만 남고 현황판에는 오르지 않는다. */
	public boolean countsTowardFulfillment() {
		return period() != null;
	}
}
