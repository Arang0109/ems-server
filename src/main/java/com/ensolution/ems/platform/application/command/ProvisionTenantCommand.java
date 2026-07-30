package com.ensolution.ems.platform.application.command;

import com.ensolution.ems.global.common.enums.SubscriptionPlan;

/**
 * 고객사 발급 커맨드. 테넌트 정보 + 초기 관리자(ADMIN) 계정을 함께 담는다.
 */
public record ProvisionTenantCommand(
	String name,
	String bizNumber,
	SubscriptionPlan subscriptionPlan,
	TenantAdminCommand admin
) {}
