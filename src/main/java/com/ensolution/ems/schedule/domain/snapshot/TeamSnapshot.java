package com.ensolution.ems.schedule.domain.snapshot;

import java.util.List;

/**
 * 측정 시점 팀 스냅샷. 그 회차에 누가 나갔고 어떤 장비를 들고 갔는지를 담는다.
 *
 * <p>장비는 유형별 슬롯이 아니라 목록으로 갖는다. 유형은 장비 자신({@code EquipmentSnapshot.type})이
 * 알고 있으므로 슬롯을 따로 두면 같은 사실이 두 곳에 적히고, 장비 유형이 늘 때마다 필드가 늘어난다.
 * 성적서의 "이 회차에 쓴 피토관/노즐" 칸은 목록에서 유형으로 골라 채운다.
 */
public record TeamSnapshot(
	Long teamId,
	String teamName,
	String mentorName,
	String menteeName,

	List<EquipmentSnapshot> equipments
) {
	/**
	 * 측정자(사수·부사수) 이름만 교체한 새 팀 스냅샷을 반환한다.
	 * 전달되지 않은(공백 포함) 이름은 기존 값을 유지한다. 원장 연결키인 {@code teamId}는
	 * 바꾸지 않으므로 문서상 표기만 바뀌고 팀 원장은 그대로다.
	 */
	public TeamSnapshot withMembers(String mentorName, String menteeName) {
		return new TeamSnapshot(
			teamId, teamName,
			SnapshotMerge.keepText(mentorName, this.mentorName),
			SnapshotMerge.keepText(menteeName, this.menteeName),
			equipments);
	}

	/**
	 * 장비 목록을 통째로 교체한 새 팀 스냅샷을 반환한다.
	 * 슬롯별 부분 갱신이 아니라 <b>전체 교체</b>다 — 호출자가 이 회차에 쓸 장비 전부를 전달한다.
	 */
	public TeamSnapshot withEquipments(List<EquipmentSnapshot> newEquipments) {
		return new TeamSnapshot(teamId, teamName, mentorName, menteeName, newEquipments);
	}
}
