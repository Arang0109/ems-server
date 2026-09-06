package com.ensolution.ems.schedule.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "측정장비 변경 요청. 전달한 목록으로 전체 교체합니다(부분 갱신이 아닙니다) — "
	+ "화면에 있는 장비 전부를 보내야 하며, 빈 목록은 '장비 없음'을 뜻합니다. "
	+ "장비 유형(입자 샘플러·가스 샘플러·피토관·노즐 등)은 서버가 장비 원장에서 판별하므로 지정하지 않습니다.")
public record ChangeScheduleEquipmentsRequest(
	@NotNull(message = "측정장비 목록은 필수 값입니다.")
	@Schema(description = "이 회차에 사용할 장비 id 목록")
	List<String> equipmentIds
) {}
