package com.ensolution.ems.schedule.presentation.response.snapshot;

/** 측정 시점 방지시설 스냅샷 응답. */
public record PreventionSnapshotResponse(
	Long preventionId,
	String name,
	Double capacity,
	String unit,
	String targetName,
	String removalEfficiency
) {}
