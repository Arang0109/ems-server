package com.ensolution.ems.schedule.domain;

import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import com.ensolution.ems.schedule.domain.sheet.SamplingPoint;
import com.ensolution.ems.schedule.domain.snapshot.BasicInfo;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;

import java.util.List;

/**
 * 세부 스냅샷의 내용으로부터 도달해야 할 진행 단계를 판정해 메타를 전진시킨다.
 * <p>
 * 진행 상태는 스냅샷에서 파생되는 값이다 — 전진만 하고 되돌아가지 않으며, 같은 스냅샷으로 몇 번
 * 호출해도 결과가 같다(멱등). 자동 전이 조건을 이 한 곳에 모아 두므로 각 유스케이스는
 * "이전 값과 비교해 최초 입력인지" 판단할 필요가 없다.
 * <p>
 * 종료(완료·취소)는 사용자 확정 사항이므로 여기서 다루지 않는다.
 */
public final class ScheduleProgress {

	private ScheduleProgress() {
	}

	/**
	 * 스냅샷 기준으로 메타를 전진시킨 결과를 반환한다. 전진할 것이 없으면 인자를 그대로 돌려준다.
	 * 측정 착수와 분석 착수를 순차 적용하므로, 시트와 접수일자가 함께 들어온 경우
	 * 측정 예정에서 분석 중까지 두 단계를 한 번에 전진한다.
	 */
	public static Schedule advance(Schedule meta, ScheduleSnapshot snapshot) {
		if (meta == null || snapshot == null || !meta.getStatus().isProgressable()) {
			return meta;
		}

		Schedule advanced = meta;
		if (measurementStarted(snapshot)) {
			advanced = advanced.startMeasuringIfScheduled();
		}
		if (sampleReceived(snapshot)) {
			advanced = advanced.startAnalyzingIfMeasuring();
		}
		return advanced;
	}

	/**
	 * 측정에 착수했는지 여부. 채취 시작시각이 입력됐거나 측정점에 실측값이 들어온 시점을 착수로 본다.
	 * 시트 틀만 만들어 둔 상태(측정점은 있으나 값이 비어 있음)는 아직 착수가 아니다.
	 */
	private static boolean measurementStarted(ScheduleSnapshot snapshot) {
		BasicInfo basicInfo = snapshot.basicInfo();
		if (basicInfo != null && basicInfo.samplingStartedAt() != null) {
			return true;
		}
		return hasMeasuredValue(snapshot.sheets());
	}

	/** 시료가 접수됐는지 여부. 접수일자 입력을 분석 착수로 본다. */
	private static boolean sampleReceived(ScheduleSnapshot snapshot) {
		BasicInfo basicInfo = snapshot.basicInfo();
		return basicInfo != null && basicInfo.receivedAt() != null;
	}

	/** 어느 시트든 측정점에 유량 실측 입력(배출가스 온도·동압·정압)이 하나라도 채워졌는지 여부. */
	private static boolean hasMeasuredValue(List<MeasurementSheet> sheets) {
		if (sheets == null) return false;
		return sheets.stream()
			.filter(sheet -> sheet != null && sheet.getSamplingPoints() != null)
			.flatMap(sheet -> sheet.getSamplingPoints().stream())
			.anyMatch(ScheduleProgress::hasMeasuredValue);
	}

	private static boolean hasMeasuredValue(SamplingPoint point) {
		return point != null
			&& (point.getTs() != null || point.getPv() != null || point.getPs() != null);
	}
}
