package com.ensolution.ems.schedule.presentation.analysis.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

/**
 * 성적서 항목별 채취시간 일괄 저장 요청. 성적서 탭이 측정항목 표를 한 번에 저장한다.
 * <p>
 * 전달한 항목만 갱신하며, 요청에 없는 항목의 채취시간은 서버 값이 그대로 남는다.
 * 반대로 <b>전달한 항목의 빈 시각은 "지웠다"는 뜻</b>이므로 기존 값을 비운다.
 * 실험실 입력값(측정분석값·단위·분석방법·분석장비)은 이 경로로 바뀌지 않는다.
 */
@Schema(description = "성적서 항목별 채취시간 일괄 저장 요청")
public record SaveSamplingTimesRequest(

	@Schema(description = "저장할 측정항목별 채취시간 목록")
	@NotNull(message = "채취시간 목록은 필수 값입니다.")
	@Valid
	List<Entry> items
) {

	@Schema(description = "측정항목 하나의 채취시간")
	public record Entry(

		@Schema(description = "측정물질 id. 이번 측정계획의 측정항목이어야 합니다.")
		@NotNull(message = "측정물질은 필수 값입니다.")
		Long pollutantId,

		@Schema(description = "채취 시작시각. 비우면 기존 값을 지웁니다.", example = "09:30:00")
		LocalTime samplingStartedAt,

		@Schema(description = "채취 종료시각. 비우면 기존 값을 지웁니다.", example = "10:00:00")
		LocalTime samplingEndedAt
	) {}
}
