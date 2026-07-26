package com.ensolution.ems.platform.presentation.response;

import com.ensolution.ems.global.common.enums.SubscriptionPlan;
import com.ensolution.ems.global.common.enums.TenantStatus;

import java.time.LocalDateTime;

public record TenantListResponse(
	Long id,
	String name,
	String bizNumber,
	TenantStatus status,
	SubscriptionPlan subscriptionPlan,
	LocalDateTime createdAt
) {}
