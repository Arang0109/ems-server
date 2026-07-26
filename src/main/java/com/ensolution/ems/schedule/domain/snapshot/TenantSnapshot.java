package com.ensolution.ems.schedule.domain.snapshot;

/** 측정 시점 고객사 스냅샷. */
public record TenantSnapshot(
	Long tenantId,
	String name,
	String bizNumber,
	String representative,
	String roadAddress,
	String detailAddress,
	String zipcode
) {}
