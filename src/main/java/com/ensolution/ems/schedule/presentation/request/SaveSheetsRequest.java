package com.ensolution.ems.schedule.presentation.request;

import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 측정 시트 저장 요청. 시트 구조가 방대하고 계산 도메인과 1:1이므로 도메인 시트를 그대로 입력받는다
 * (presentation → domain 참조는 의존 방향에 부합). 저장 시 서버가 계산 파이프라인을 실행한다.
 */
public record SaveSheetsRequest(
	@NotNull(message = "측정 시트는 필수 값입니다.")
	List<MeasurementSheet> sheets
) {}
