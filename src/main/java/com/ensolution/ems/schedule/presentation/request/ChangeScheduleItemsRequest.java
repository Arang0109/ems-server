package com.ensolution.ems.schedule.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "측정계획 측정항목 변경 요청. 이번 계획에서 측정할 측정물질을 전달한 목록으로 전체 교체합니다.")
public record ChangeScheduleItemsRequest(
	@Schema(description = "측정할 측정물질 id 목록. 측정시설에 등록된 측정항목 중에서만 선택할 수 있습니다.")
	@NotEmpty(message = "측정 항목은 하나 이상 선택해야 합니다.")
	List<Long> pollutantIds
) {}
