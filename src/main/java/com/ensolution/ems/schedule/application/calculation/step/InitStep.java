package com.ensolution.ems.schedule.application.calculation.step;

import com.ensolution.ems.schedule.application.calculation.Calculator;
import com.ensolution.ems.schedule.application.calculation.SheetContext;
import com.ensolution.ems.schedule.domain.sheet.MeasurementPoint;
import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** 측정점 평균(배출가스 절대온도 avgTg, 평균 동압 avgPv, 평균 정압 avgPs)을 계산한다. */
@Component
@Order(1)
@RequiredArgsConstructor
public class InitStep implements SheetStep {

	private final Calculator calculator;

	@Override
	public void execute(SheetContext context) {
		MeasurementSheet sheet = context.getSheet();
		List<MeasurementPoint> points = sheet.getMeasurementPoints();
		if (points == null || points.isEmpty()) return;

		BigDecimal avgTg = calculator.averageTreatNullAsZero(
			points.stream()
				.map(p -> p.getTs() == null ? null : p.getTs().add(BigDecimal.valueOf(273)))
				.toList(), 1
		);
		BigDecimal avgPv = calculator.averageTreatNullAsZero(
			points.stream().map(MeasurementPoint::getPv).toList(), 1
		);
		BigDecimal avgPs = calculator.averageTreatNullAsZero(
			points.stream().map(MeasurementPoint::getPs).toList(), 1
		);

		context.setAvgTg(avgTg);
		context.setAvgPv(avgPv);
		context.setAvgPs(avgPs);
	}
}
