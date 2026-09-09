package com.ensolution.ems.schedule.application.command.update;

/**
 * 측정계획 문서의 팀 스냅샷 수정 커맨드. 이 경로가 소유하는 것은 <b>측정자 표기</b>뿐이다.
 *
 * <p>전달되지 않은(공백 포함) 이름은 기존 값을 유지하며, 팀 원장과 이 회차에 들고 간 장비는
 * 바뀌지 않는다(장비 교체는 {@link ChangeScheduleEquipmentsCommand} 경로를 쓴다).
 */
public record ChangeTeamSnapshotCommand(
	String mentorName,
	String menteeName
) {}
