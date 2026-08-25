package com.ensolution.ems.schedule.domain.snapshot;

final class SnapshotMerge {

	private SnapshotMerge() {}

	static <T> T keep(T value, T original) {
		return value == null ? original : value;
	}

	static String keepText(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}
}
