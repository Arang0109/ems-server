package com.ensolution.ems.schedule.domain.snapshot;

/**
 * 스냅샷 부분 병합(patch) 공통 규칙.
 * 스냅샷 트리(의뢰기관→사업장→측정시설)의 각 레코드가 동일한 규칙으로 병합되도록 한 곳에 모아 둔다.
 */
final class SnapshotMerge {

	private SnapshotMerge() {}

	/** patch 값이 null이면 기존 값을 유지한다. */
	static <T> T keep(T value, T original) {
		return value == null ? original : value;
	}

	/** patch 값이 null이거나 공백이면 기존 값을 유지한다. */
	static String keepText(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}
}
