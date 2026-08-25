package com.ensolution.ems.platform.domain;

public enum TenantStatus {
	ACTIVE,      // 정상 이용
	SUSPENDED,   // 이용 정지(미납, 관리자 조치)
	INACTIVE,    // 계약 종료(해지)
	PENDING      // 가입 후 승인 대기
}
