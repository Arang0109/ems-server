package com.ensolution.ems.schedule.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 회차 커스텀 필드 값 저장 요청. <b>전체 채택</b>이다 — 이 폼이 단독 소유하는 경로라 정의된 필드 전부를 보내며,
 * 빠진 키와 빈 값은 "지웠다"로 읽는다. 키는 {@code GET /api/schedules/custom-fields}의 정의에 있어야 한다.
 */
@Schema(description = "회차 커스텀 필드 값 저장 요청. 정의된 필드 전부를 보내며 빠진 키·빈 값은 지워집니다.")
public record SaveScheduleCustomFieldsRequest(
	@Schema(description = "키(정의된 커스텀 필드 key) → 값. 템플릿이 ${custom.<key>}로 읽습니다.",
		example = "{\"siteCode\": \"A-01\", \"inspector\": \"홍길동\"}")
	@NotNull
	Map<String, @Size(max = 500) String> values
) {}
