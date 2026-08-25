package com.ensolution.ems.schedule.domain.history;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

/**
 * 한 측정항목이 한 주기 구간을 이행했는지에 대한 판정.
 *
 * <p>미이행을 {@code PENDING}(아직 기한 전)과 {@code OVERDUE}(기한 경과)로 가르는 이유는,
 * 아직 오지 않은 구간까지 미이행으로 표시하면 연초에 현황판이 온통 경고로 뒤덮여 정작 놓친 구간이
 * 묻히기 때문이다. 조치가 필요한 것은 기한이 지난 구간뿐이다.
 *
 * <p>{@code PARTIAL}은 월 2회 주기({@code TWICE_MONTHLY})에서만 나온다 — 한 번만 측정한 달은
 * 이행도 미이행도 아니며 남은 1회를 그달 안에 채워야 한다. 기한이 이미 지났다면 부분 이행이라도
 * 조치 대상이므로 {@code OVERDUE}로 넘긴다.
 */
@Getter
@RequiredArgsConstructor
public enum FulfillmentStatus {
	FULFILLED("이행"),
	PARTIAL("일부 이행"),
	PENDING("이행 예정"),
	OVERDUE("기한 경과");

	private final String description;

	/**
	 * 기록 수와 필요 횟수를 견줘 판정한다. 필요 횟수를 넘겨 측정한 경우도 이행이다.
	 *
	 * @param baseDate 판정 기준일(보통 오늘). null이면 기한을 따지지 않고 미이행을 {@code PENDING}으로 둔다
	 */
	public static FulfillmentStatus of(MeasurementPeriod period, int fulfilledCount, LocalDate baseDate) {
		if (fulfilledCount >= period.requiredCount()) return FULFILLED;
		if (period.isOverdueAt(baseDate)) return OVERDUE;
		if (fulfilledCount > 0) return PARTIAL;
		return PENDING;
	}

	public boolean isFulfilled() {
		return this == FULFILLED;
	}

	/** 조치가 필요한 상태인지 여부(미이행 목록의 필터 조건). */
	public boolean needsAction() {
		return this != FULFILLED;
	}
}
