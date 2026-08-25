package com.ensolution.ems.platform.presentation.response;

import com.ensolution.ems.platform.domain.SubscriptionPlan;
import com.ensolution.ems.platform.domain.TenantStatus;

import java.time.LocalDateTime;

public record TenantResponse(
	Long id,
	String name,
	String bizNumber,
	TenantStatus status,
	SubscriptionPlan subscriptionPlan,
	LocalDateTime createdAt,
	LocalDateTime modifiedAt
) {}
