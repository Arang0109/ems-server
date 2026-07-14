package com.ensolution.ems.schedule.domain;

/**
 * 측정계획 상태. 상태 전이 규칙과 편집 가능 여부를 도메인 규칙으로 캡슐화한다.
 * schedule 모듈 특화 개념이므로 global 공통 enum이 아닌 모듈 내부에 둔다.
 */
public enum ScheduleStatus {
	SCHEDULED,   // 측정 예정
	MEASURING,   // 측정 중
	ANALYZING,   // 분석 중
	COMPLETED,   // 완료
	CANCELED;    // 취소

	/** 메타·세부 정보를 편집할 수 있는 상태인지 여부(완료·취소는 잠금). */
	public boolean canEdit() {
		return this != COMPLETED && this != CANCELED;
	}

	/** 현재 상태에서 대상 상태로의 전이가 허용되는지 여부. */
	public boolean canTransitionTo(ScheduleStatus next) {
		if (next == null) return false;
		return switch (this) {
			case SCHEDULED -> next == MEASURING || next == CANCELED;
			case MEASURING -> next == ANALYZING || next == CANCELED;
			case ANALYZING -> next == COMPLETED || next == CANCELED;
			case COMPLETED, CANCELED -> false;
		};
	}
}
