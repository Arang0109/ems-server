package com.ensolution.ems.schedule.domain.snapshot;

/** 측정 시점 팀 스냅샷. */
public record TeamSnapshot(
	Long teamId,
	String teamName,
	Long mentorUserId,
	String mentorName,
	Long menteeUserId,
	String menteeName,
	String particleSamplerId,
	String gasSamplerId,
	String pitotTubeId,
	String nozzleId
) {
	/**
	 * 측정자(멘토·멘티) 이름만 교체한 새 팀 스냅샷을 반환한다. 전달되지 않은(공백 포함) 이름은 기존 값을 유지한다.
	 * 원장 연결키인 {@code teamId}·{@code mentorUserId}·{@code menteeUserId}는 변경하지 않으므로,
	 * 문서상 표기만 바꾸고 팀 원장은 건드리지 않는다.
	 */
	public TeamSnapshot withMembers(String mentorName, String menteeName) {
		return new TeamSnapshot(
			teamId, teamName, mentorUserId,
			SnapshotMerge.keepText(mentorName, this.mentorName),
			menteeUserId,
			SnapshotMerge.keepText(menteeName, this.menteeName),
			particleSamplerId, gasSamplerId, pitotTubeId, nozzleId);
	}

	/** 장비 id만 교체한 새 팀 스냅샷을 반환한다. 전달되지 않은 슬롯은 기존 장비를 유지한다. */
	public TeamSnapshot withEquipmentIds(String particleSamplerId, String gasSamplerId,
	                                     String pitotTubeId, String nozzleId) {
		return new TeamSnapshot(
			teamId, teamName, mentorUserId, mentorName, menteeUserId, menteeName,
			SnapshotMerge.keepText(particleSamplerId, this.particleSamplerId),
			SnapshotMerge.keepText(gasSamplerId, this.gasSamplerId),
			SnapshotMerge.keepText(pitotTubeId, this.pitotTubeId),
			SnapshotMerge.keepText(nozzleId, this.nozzleId));
	}
}
