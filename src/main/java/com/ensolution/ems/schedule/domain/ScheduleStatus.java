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

	/** 더 이상 전진하지 않는 종단 상태(완료·취소)인지 여부. */
	public boolean isTerminal() {
		return this == COMPLETED || this == CANCELED;
	}

	/** 메타·세부 정보를 편집할 수 있는 상태인지 여부(완료·취소는 잠금). */
	public boolean canEdit() {
		return !isTerminal();
	}

	/**
	 * 자동 전진(측정 착수·분석 착수) 대상 상태인지 여부.
	 * 종단 상태는 스냅샷에 무엇이 입력되든 전진시키지 않는다.
	 */
	public boolean isProgressable() {
		return !isTerminal();
	}

	/**
	 * 종단 상태를 되돌려 다시 작업 가능하게 만들 수 있는지 여부.
	 * 일반 전이({@link #canTransitionTo})와 분리해 둔다 — 재개방은 실수를 바로잡는 예외 경로이므로
	 * 종단 상태에서의 일반 전이를 열어 주면 누구나 아무 때나 완료·취소를 되돌릴 수 있게 된다.
	 */
	public boolean canReopen() {
		return isTerminal();
	}

	/**
	 * 재개방에 관리자 권한이 필요한 상태인지 여부.
	 * <p>
	 * 완료는 성적서 발행까지 끝난 대외 확정이므로 관리자만 되돌린다. 취소는 진행 중이던 계획을
	 * 되살리는 것이고, 실수로 취소하면 이미 입력한 측정 데이터가 잠겨 담당자가 즉시 바로잡아야 하므로
	 * 권한을 요구하지 않는다.
	 */
	public boolean requiresAdminToReopen() {
		return this == COMPLETED;
	}

	/**
	 * 계획을 삭제(감춤)할 수 있는 상태인지 여부.
	 * 실측 데이터가 아직 없는 '측정 예정'만 등록 실수로 보고 삭제를 허용하며,
	 * 측정에 착수한 이후에는 취소만 가능하다.
	 */
	public boolean canDelete() {
		return this == SCHEDULED;
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
