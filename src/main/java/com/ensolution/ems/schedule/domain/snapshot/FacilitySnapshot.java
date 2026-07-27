package com.ensolution.ems.schedule.domain.snapshot;

/** 측정 시점 배출시설 스냅샷. */
public record FacilitySnapshot(
	Long facilityId,
	String name,
	String fuelUsage,
	String productOutput,
	String incinerationAmount,
	String fuelInput,
	String fuelType,
	String unit
) {}
