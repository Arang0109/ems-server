package com.ensolution.ems.schedule.application.calculation.step;

import com.ensolution.ems.schedule.application.calculation.SheetContext;

/** 측정 시트 계산 파이프라인의 단일 단계. {@code @Order}로 실행 순서를 지정한다. */
public interface SheetStep {
	void execute(SheetContext context);
}
