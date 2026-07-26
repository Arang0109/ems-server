package com.ensolution.ems.schedule.application.command.update;

/**
 * 측정계획의 측정장비 교체 커맨드. 장비 spec별 id를 담으며,
 * null·공백인 슬롯은 기존 장비를 유지한다.
 */
public record ChangeScheduleEquipmentsCommand(
	String particleSamplerId,
	String gasSamplerId,
	String pitotTubeId,
	String nozzleId
) {}
