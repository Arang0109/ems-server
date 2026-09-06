package com.ensolution.ems.schedule.domain;

import com.ensolution.ems.schedule.domain.sampling.SamplingPoint;
import com.ensolution.ems.schedule.domain.sampling.SamplingSheet;
import com.ensolution.ems.schedule.domain.snapshot.SamplingSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 저장된 내용을 근거로 측정계획의 진행 단계를 자동으로 전진시킨다.
 *
 * <p>근거가 두 저장소에 나뉘어 있다 — 채취 착수는 문서(채취 시작시각·측정점 실측값)에서,
 * 시료 접수는 메타(접수일자)에서 읽는다. 접수일자는 성적서 진행 값이라 메타가 진실이기 때문이다.
 *
 * <p>전진은 <b>분석값 입력 중까지만</b> 한다 — 성적서 작성 완료는 사용자가 확정하는 경계이며,
 * 그래서 완료 훅을 사용자 확정 경로 한 곳에만 두면 충분하다.
 */
@NoArgsConstructor
public final class ScheduleProgress {

	public static Schedule advance(Schedule meta, ScheduleSnapshot snapshot) {
		if (meta == null || snapshot == null || !meta.getStatus().canAutoAdvanced()) { return meta; }

		Schedule advanced = meta;
		if (measurementStarted(snapshot)) { advanced = advanced.startMeasuring(); }
		if (sampleReceived(meta)) { advanced = advanced.startAnalyzing(); }
		return advanced;
	}

	/** 채취 시작시각이 적혔거나, 어느 기록지든 실측값이 들어왔으면 측정에 착수한 것으로 본다. */
	private static boolean measurementStarted(ScheduleSnapshot snapshot) {
		SamplingSnapshot sampling = snapshot.samplingData();
		if (sampling != null && sampling.samplingStartedAt() != null) {
			return true;
		}
		return hasMeasuredValue(snapshot.sheets());
	}

	/** 시료가 접수됐는지 여부. 접수일자 입력을 분석 착수로 본다. 접수일자의 진실은 메타다. */
	private static boolean sampleReceived(Schedule meta) {
		return meta.getReceivedAt() != null;
	}

	/** 어느 시트든 측정점에 유량 실측 입력(배출가스 온도·동압·정압)이 하나라도 채워졌는지 여부. */
	private static boolean hasMeasuredValue(List<SamplingSheet> sheets) {
		if (sheets == null) return false;
		return sheets.stream()
			.filter(sheet -> sheet != null && sheet.getSamplingPoints() != null)
			.flatMap(sheet -> sheet.getSamplingPoints().stream())
			.anyMatch(ScheduleProgress::hasMeasuredValue);
	}

	private static boolean hasMeasuredValue(SamplingPoint point) {
		return point != null
			&& (point.getGasTemperature() != null
			|| point.getDynamicPressure() != null
			|| point.getStaticPressure() != null);
	}
}
