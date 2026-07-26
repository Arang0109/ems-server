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
