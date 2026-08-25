package com.ensolution.ems.schedule.domain;

import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import com.ensolution.ems.schedule.domain.sheet.SamplingPoint;
import com.ensolution.ems.schedule.domain.snapshot.BasicInfo;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
public final class ScheduleProgress {
	
	public static Schedule advance(Schedule meta, ScheduleSnapshot snapshot) {
		if (meta == null || snapshot == null || !meta.getStatus().canAutoAdvanced()) {
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
