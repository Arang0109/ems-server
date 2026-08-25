package com.ensolution.ems.schedule.presentation.request;

import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import com.ensolution.ems.schedule.domain.sheet.SheetRef;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 측정 시트 저장 요청. 시트 구조가 방대하고 계산 도메인과 1:1이므로 도메인 시트를 그대로 입력받는다
 * (presentation → domain 참조는 의존 방향에 부합). 저장 시 서버가 계산 파이프라인을 실행한다.
 * <p>
 * 각 시트는 읽어간 시점의 {@code version}을 지녀야 하며, 서버는 이 값으로 동시 편집 충돌을 판정한다.
 * 요청에 담기지 않은 카테고리의 시트는 서버 값이 그대로 유지되므로, 시트 삭제는
 * {@code deletedSheets}로 명시해야 한다 — 그래야 내가 화면을 연 뒤 다른 사용자가 추가한
 * 시트가 내 저장으로 조용히 사라지지 않는다.
 */
public record SaveSheetsRequest(
	@NotNull(message = "측정 시트는 필수 값입니다.")
	List<MeasurementSheet> sheets,

	List<SheetRef> deletedSheets
) {}
