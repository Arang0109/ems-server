package com.ensolution.ems.platform.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 고객사(테넌트) 애그리거트. 플랫폼 운영자(PLATFORM_ADMIN)가 생명주기를 관리한다.
 * JPA 영속 앵커인 TenantEntity는 tenant 모듈에 있으며(멀티테넌시 공용), 본 도메인 모델은 platform 모듈이 소유한다.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder(toBuilder = true)
public class Tenant {
	private Long id;
	private String name;
	private String bizNumber;
	private String representative;
	private String roadAddress;
	private String detailAddress;
	private String zipcode;
	/** 시료분석검사자. 성적서 서명란의 기본값이며, 회차별로 다르면 측정계획 스냅샷에서 덮어쓴다. */
	private String analyst;
	/** 기술책임자. 위와 같은 규약이다. */
	private String technicalManager;
	private TenantStatus status;
	private SubscriptionPlan subscriptionPlan;
	private LocalDateTime createdAt;
	private LocalDateTime modifiedAt;

	/**
	 * 신규 고객사 발급. 기본 상태는 ACTIVE.
	 */
	public static Tenant provision(
		String name, String bizNumber, SubscriptionPlan subscriptionPlan
	) {
		return Tenant.builder()
			.name(name)
			.bizNumber(bizNumber)
			.status(TenantStatus.ACTIVE)
			.subscriptionPlan(subscriptionPlan)
			.build();
	}

	/**
	 * 플랫폼 운영자 계정이 소속되는 전용 시스템 테넌트. 구독 플랜은 내부용(INTERNAL).
	 */
	public static Tenant system(String name, String bizNumber) {
		return Tenant.builder()
			.name(name)
			.bizNumber(bizNumber)
			.status(TenantStatus.ACTIVE)
			.subscriptionPlan(SubscriptionPlan.INTERNAL)
			.build();
	}
}
