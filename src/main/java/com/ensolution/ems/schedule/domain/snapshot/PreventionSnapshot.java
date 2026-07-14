package com.ensolution.ems.schedule.domain.snapshot;

import java.util.List;

/** 측정 시점 방지시설 스냅샷. 하위로 측정대상물질 스냅샷을 품는다. */
public record PreventionSnapshot(
	Long preventionId,
	String name,
	List<TargetSubstanceSnapshot> targetSubstances
) {}
