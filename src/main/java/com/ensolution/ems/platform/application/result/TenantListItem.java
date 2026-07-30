package com.ensolution.ems.platform.application.result;

import com.ensolution.ems.global.common.enums.SubscriptionPlan;
import com.ensolution.ems.global.common.enums.TenantStatus;

import java.time.LocalDateTime;

/**
 * 고객사 목록 조회 아이템 VO.
 */
public record TenantListItem(
	Long id,
	String name,
	String bizNumber,
	TenantStatus status,
	SubscriptionPlan subscriptionPlan,
	LocalDateTime createdAt
) {}
