package com.ensolution.ems.schedule.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 측정계획 측정항목 순서 변경 요청.
 *
 * <p>{@code orderedPollutantIds} 는 이 계획의 측정항목 <b>전체</b>여야 한다. 배열 순서가 곧 성적서 표기 순서다.
 * 서버가 보유한 집합과 정확히 일치하지 않으면 — 빠진 id, 이 계획에 없는 id, 중복 id 중 하나라도 있으면 —
 * 아무것도 저장하지 않고 거절한다.
 *
 * <p>"요청에 없는 항목은 서버 값을 유지"를 택하지 않은 이유: 순서는 집합 전체에 대한 전순서라
 * 일부만 재배열하면 나머지 항목이 어디에 놓이는지 정의되지 않는다.
 *
 * <p>측정항목 자체를 더하거나 빼려면 순서 변경이 아니라 {@code PATCH /api/schedules/{scheduleId}/items} 를 쓴다.
 */
@Schema(description = "측정계획 측정항목 순서 변경 요청. 배열 순서가 곧 성적서 표기 순서입니다.")
public record ReorderScheduleItemsRequest(
	@Schema(description = "측정물질 id 목록. 이 계획의 측정항목 전체여야 하며, 배열 순서가 곧 성적서 표기 순서입니다.")
	@NotEmpty(message = "측정 항목은 하나 이상 선택해야 합니다.")
	List<Long> orderedPollutantIds
) {}
