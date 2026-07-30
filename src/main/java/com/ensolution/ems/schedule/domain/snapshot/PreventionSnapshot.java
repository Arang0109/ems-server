package com.ensolution.ems.schedule.domain.snapshot;

/** 측정 시점 방지시설 스냅샷. 대상물질명·제거효율을 자체 필드로 함께 보관한다. */
public record PreventionSnapshot(
	Long preventionId,
	String name,
	Double capacity,
	String targetName,
	String removalEfficiency
) {}
