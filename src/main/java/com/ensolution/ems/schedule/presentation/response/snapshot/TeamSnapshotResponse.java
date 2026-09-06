package com.ensolution.ems.schedule.presentation.response.snapshot;

import java.util.List;

/**
 * 측정 시점 팀 스냅샷 응답. 이 회차에 나간 측정자와 들고 간 장비를 담는다.
 * 측정자 이름은 문서상 표기이며 팀 원장과 다를 수 있다.
 */
public record TeamSnapshotResponse(
	Long teamId,
	String teamName,
	String mentorName,
	String menteeName,
	List<EquipmentSnapshotResponse> equipments
) {}
