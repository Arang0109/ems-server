package com.ensolution.ems.schedule.application.calculation.step;

import com.ensolution.ems.global.common.enums.Shape;
import com.ensolution.ems.schedule.application.calculation.Calculator;
import com.ensolution.ems.schedule.application.calculation.SheetContext;
import com.ensolution.ems.schedule.domain.sheet.SamplingPoint;
import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 측정시설 단면적(area)과 규정상 요구 측정점 수(samplingPointCnt), 측정점 평균(avgTg·avgPv·avgPs)을 계산한다.
 * 단면적·측정점 수는 측정시설(굴뚝) 형상·치수에서, 평균은 측정점 입력값에서 구한다.
 * 측정점 수는 클라이언트가 보낸 배열 길이가 아니라 굴뚝 단면 크기에서 서버가 결정적으로 산출한다.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class InitStep implements SheetStep {

	private static final BigDecimal PI = BigDecimal.valueOf(Math.PI);

	private final Calculator calculator;

	@Override
	public void execute(SheetContext context) {
		setArea(context);
		setSamplingPointCnt(context);
		setPointAverages(context);
	}

	// 측정시설 단면적: 원형 π·d²/4, 사각형 가로×세로 (치수 단위 m → area m²)
	private void setArea(SheetContext context) {
		Shape shape = context.getStackShape();
		BigDecimal horizontal = context.getHorizontalLength();
		if (shape == null || horizontal == null) return;

		BigDecimal area;
		if (shape == Shape.CIRCULAR) {
			area = PI.multiply(horizontal).multiply(horizontal)
				.divide(BigDecimal.valueOf(4), 5, RoundingMode.HALF_UP);
		} else {
			BigDecimal vertical = context.getVerticalLength();
			if (vertical == null) return;
			area = horizontal.multiply(vertical);
		}
		context.setArea(calculator.round(area, 3));
	}

	// 규정상 요구 측정점 수: 원형은 지름 구간별, 사각형은 1
	private void setSamplingPointCnt(SheetContext context) {
		Shape shape = context.getStackShape();
		BigDecimal diameter = context.getHorizontalLength();
		if (shape == null || diameter == null) return;

		if (shape != Shape.CIRCULAR) {
			context.setSamplingPointCnt(1);
			return;
		}
		int cnt;
		if (diameter.compareTo(BigDecimal.ONE) <= 0) cnt = 1;
		else if (diameter.compareTo(BigDecimal.valueOf(2)) <= 0) cnt = 2;
		else if (diameter.compareTo(BigDecimal.valueOf(4)) <= 0) cnt = 3;
		else if (diameter.compareTo(BigDecimal.valueOf(4.5)) <= 0) cnt = 4;
		else cnt = 5;
		context.setSamplingPointCnt(cnt);
	}

	private void setPointAverages(SheetContext context) {
		MeasurementSheet sheet = context.getSheet();
		List<SamplingPoint> points = sheet.getSamplingPoints();
		if (points == null || points.isEmpty()) return;

		BigDecimal avgTg = calculator.averageTreatNullAsZero(
			points.stream()
				.map(p -> p.getTs() == null ? null : p.getTs().add(BigDecimal.valueOf(273)))
				.toList(), 1
		);
		BigDecimal avgPv = calculator.averageTreatNullAsZero(
			points.stream().map(SamplingPoint::getPv).toList(), 1
		);
		BigDecimal avgPs = calculator.averageTreatNullAsZero(
			points.stream().map(SamplingPoint::getPs).toList(), 1
		);

		context.setAvgTg(avgTg);
		context.setAvgPv(avgPv);
		context.setAvgPs(avgPs);
	}
}
