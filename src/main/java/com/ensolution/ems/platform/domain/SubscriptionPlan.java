package com.ensolution.ems.platform.domain;

public enum SubscriptionPlan {
	BASIC,
	PRO,
	ENTERPRISE,
	
	/**
	 * 개발 및 테스트 전용.
	 * 모든 기능 사용 가능.
	 */
	INTERNAL
}