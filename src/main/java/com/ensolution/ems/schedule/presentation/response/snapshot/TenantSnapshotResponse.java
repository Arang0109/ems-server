package com.ensolution.ems.schedule.presentation.response.snapshot;

/** 측정 시점 고객사 스냅샷 응답. */
public record TenantSnapshotResponse(
	Long tenantId,
	String name,
	String bizNumber,
	String representative,
	String roadAddress,
	String detailAddress,
	String zipcode
) {}
