package com.ensolution.ems.schedule.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "측정장비 변경 요청. 전달하지 않은 장비는 기존 값을 유지합니다.")
public record ChangeScheduleEquipmentsRequest(
	@Schema(description = "입자 샘플러 장비 id")
	String particleSamplerId,

	@Schema(description = "가스 샘플러 장비 id")
	String gasSamplerId,

	@Schema(description = "피토관 장비 id")
	String pitotTubeId,

	@Schema(description = "노즐 장비 id")
	String nozzleId
) {}
