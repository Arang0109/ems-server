package com.ensolution.ems.schedule.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "측정팀 스냅샷 수정 요청. 전달하지 않은(공백 포함) 이름은 기존 값을 유지합니다. "
	+ "이 회차 문서의 표기만 바뀌며 팀 원장과 장비 목록은 그대로입니다.")
public record ChangeTeamSnapshotRequest(
	@Schema(description = "측정자(사수) 표기명. 팀 원장은 바뀌지 않습니다.")
	String mentorName,

	@Schema(description = "측정자(부사수) 표기명. 팀 원장은 바뀌지 않습니다.")
	String menteeName
) {}
