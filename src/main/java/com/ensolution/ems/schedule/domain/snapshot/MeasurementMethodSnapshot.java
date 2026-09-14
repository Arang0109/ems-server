package com.ensolution.ems.schedule.domain.snapshot;

import com.ensolution.ems.global.common.enums.SampleGrouping;

/**
 * 측정 시점 측정방법 사본. 원장({@code client_management.MeasurementMethod})의 채취 단위·통칭 시료명·
 * 표준 채취시간을 복사해 두어, 이후 고객사가 측정방법을 고쳐도 이 회차의 기록지가 흔들리지 않는다.
 *
 * <p>현장 기록지의 가스상 시료 표는 이 값으로 행을 만든다 — {@code MERGED}면 그 방법의 항목 전부를
 * {@code mergedSampleName} 한 행으로, {@code PER_ITEM}이면 항목별 한 행으로, {@code NONE}이면 행을 만들지 않는다.
 * 실제로 어느 병에 어떤 항목이 담겼는지는 규칙이 아니라 사실이므로 {@code GaseousSampling.pollutantIds}가 따로 기록한다.
 *
 * @param methodId 원장 연결키. 2026-09-14 이전 문서는 마이그레이션이 enum 문자열을 이 사본으로 바꾼 것이라
 *                 null이다 — 소비처는 {@code methodId ?? name}으로 그룹을 식별해야 한다
 */
public record MeasurementMethodSnapshot(
	Long methodId,
	String name,
	SampleGrouping sampleGrouping,
	String mergedSampleName,
	Integer samplingMinutes
) {}
