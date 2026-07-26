package com.ensolution.ems.schedule.application.calculation;

import com.ensolution.ems.equipment.domain.spec.NozzleSpec;
import com.ensolution.ems.equipment.domain.spec.PitotTubeSpec;
import com.ensolution.ems.schedule.application.calculation.step.SheetStep;
import com.ensolution.ems.schedule.application.command.StackData;
import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 측정 시트 계산 파이프라인 진입점. {@code @Order}로 정렬된 {@link SheetStep}들을 순서대로 실행해
 * 계산 결과가 반영된 새 시트를 반환한다.
 */
@Component
@RequiredArgsConstructor
public class SheetCalculator {

	private final List<SheetStep> steps;

	public MeasurementSheet calculate(
		MeasurementSheet sheet,
		StackData stackData,
		List<PitotTubeSpec.PitotCoefficient> pitotCoefficients,
		List<NozzleSpec.NozzleDiameter> diameters,
		BigDecimal deltaH
	) {
		SheetContext context = new SheetContext(sheet, stackData, pitotCoefficients, diameters, deltaH);
		for (SheetStep step : steps) {
			step.execute(context);
		}
		return context.getSheet();
	}
}
