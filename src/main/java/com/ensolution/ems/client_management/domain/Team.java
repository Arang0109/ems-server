package com.ensolution.ems.client_management.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Team {
	private Long id;
	private Long tenantId;
	private String name;
	private Long mentorUserId;
	private Long menteeUserId;
	private String particleSamplerId;
	private String gasSamplerId;
	private String pitotTubeId;
	private String nozzleId;

	public static Team register(
		Long tenantId,
		String name,
		Long mentorUserId,
		Long menteeUserId,
		String particleSamplerId,
		String gasSamplerId,
		String pitotTubeId,
		String nozzleId
	) {
		return Team.builder()
			.tenantId(tenantId)
			.name(name)
			.mentorUserId(mentorUserId)
			.menteeUserId(menteeUserId)
			.particleSamplerId(particleSamplerId)
			.gasSamplerId(gasSamplerId)
			.pitotTubeId(pitotTubeId)
			.nozzleId(nozzleId)
			.build();
	}

	public Team update(
		Long id,
		String name,
		Long mentorUserId,
		Long menteeUserId,
		String particleSamplerId,
		String gasSamplerId,
		String pitotTubeId,
		String nozzleId
	) {
		return this.toBuilder()
			.id(id)
			.name(keep(name, this.name))
			.mentorUserId(keep(mentorUserId, this.mentorUserId))
			.menteeUserId(keep(menteeUserId, this.menteeUserId))
			.particleSamplerId(keep(particleSamplerId, this.particleSamplerId))
			.gasSamplerId(keep(gasSamplerId, this.gasSamplerId))
			.pitotTubeId(keep(pitotTubeId, this.pitotTubeId))
			.nozzleId(keep(nozzleId, this.nozzleId))
			.build();
	}

	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}

	private static <T> T keep(T value, T original) {
		return value == null ? original : value;
	}
}
