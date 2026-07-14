package com.ensolution.ems.schedule.domain.snapshot;

import com.ensolution.ems.global.common.enums.Grade;

/** 측정 시점 사업장 스냅샷. 하위로 측정시설 스냅샷을 품는다. */
public record WorkplaceSnapshot(
	Long workplaceId,
	String name,
	String bizNumber,
	String roadAddress,
	String detailAddress,
	String zipcode,
	Grade grade,
	StackSnapshot stack
) {}
