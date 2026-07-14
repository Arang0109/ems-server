package com.ensolution.ems.schedule.domain.snapshot;

/** 측정 시점 측정대상물질 스냅샷. */
public record TargetSubstanceSnapshot(
	Long targetSubstanceId,
	String name,
	Double removalEfficiency
) {}
