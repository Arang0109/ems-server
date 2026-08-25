package com.ensolution.ems.schedule.domain;

public enum ScheduleStatus {
	SCHEDULED,          // 측정 예정
	MEASURING,          // 측정 중
	ANALYZING,          // 분석값 입력 중 (시료를 인계받은 시점부터)
	REPORT_COMPLETED,   // 성적서 작성 완료 (분석 완료와 같은 시점)
	CANCELED;           // 취소

	public boolean isTerminal() {
		return this == REPORT_COMPLETED || this == CANCELED;
	}

	public boolean canEdit() {
		return !isTerminal();
	}
	
	public boolean canAutoAdvanced() { return !isTerminal(); }

	public boolean canReopen() {
		return isTerminal();
	}

	public boolean canDelete() {
		return this == SCHEDULED || this == CANCELED;
	}

	/** 현재 상태에서 대상 상태로의 전이가 허용되는지 여부. */
	public boolean canTransitionTo(ScheduleStatus next) {
		if (next == null) return false;
		return switch (this) {
			case SCHEDULED -> next == MEASURING || next == CANCELED;
			case MEASURING -> next == ANALYZING || next == CANCELED;
			case ANALYZING -> next == REPORT_COMPLETED || next == CANCELED;
			case REPORT_COMPLETED, CANCELED -> false;
		};
	}
}
