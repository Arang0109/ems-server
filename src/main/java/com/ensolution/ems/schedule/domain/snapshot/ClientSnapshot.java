package com.ensolution.ems.schedule.domain.snapshot;

/** 측정 시점 의뢰기관 스냅샷. 하위로 사업장 스냅샷을 품는 트리 루트. */
public record ClientSnapshot(
	Long clientId,
	String name,
	String bizNumber,
	String representative,
	String roadAddress,
	String detailAddress,
	String zipcode,
	String manager,
	String email,
	String tel,
	WorkplaceSnapshot workplace
) {}
