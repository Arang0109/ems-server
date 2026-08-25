package com.ensolution.ems.schedule.domain.sheet;

/**
 * 시트 참조. 삭제처럼 시트 본문 없이 대상만 가리켜야 하는 요청에서 쓴다.
 * 삭제도 편집이므로 {@code version}을 함께 받아 충돌을 판정한다 —
 * 내가 화면을 연 뒤 다른 사용자가 그 시트에 값을 입력했다면 삭제는 거부되어야 한다.
 */
public record SheetRef(
	MeasurementCategory category,
	Long version
) {}
