package com.ensolution.ems.schedule.presentation.response.snapshot;

/** 측정 시점 배출시설 스냅샷 응답. */
public record FacilitySnapshotResponse(
	Long facilityId,
	String name,
	String fuelUsage,
	String productOutput,
	String incinerationAmount,
	String fuelInput,
	String fuelType,
	String unit
) {}
