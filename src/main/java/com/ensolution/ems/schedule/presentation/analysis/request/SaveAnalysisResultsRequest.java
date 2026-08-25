package com.ensolution.ems.schedule.presentation.analysis.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * 항목별 실험분석 결과 일괄 저장 요청. 실험·분석 탭이 측정항목 표를 한 번에 저장한다.
 * <p>
 * 전달한 항목만 갱신하며, 요청에 없는 항목은 서버 값이 그대로 남는다.
 * 반대로 <b>전달한 항목의 빈 값은 "지웠다"는 뜻</b>이므로 기존 값을 비운다.
 * 채취시간은 이 경로로 바뀌지 않는다(성적서 탭 소유).
 */
@Schema(description = "항목별 실험분석 결과 일괄 저장 요청")
public record SaveAnalysisResultsRequest(

	@Schema(description = "저장할 측정항목별 분석 결과 목록")
	@NotNull(message = "분석 결과 목록은 필수 값입니다.")
	@Valid
	List<Entry> items
) {

	@Schema(description = "측정항목 하나의 실험분석 결과")
	public record Entry(

		@Schema(description = "측정물질 id. 이번 측정계획의 측정항목이어야 합니다.")
		@NotNull(message = "측정물질은 필수 값입니다.")
		Long pollutantId,

		@Schema(description = "측정분석값. 비우면 기존 값을 지웁니다.")
		BigDecimal analysisValue,

		@Schema(description = "측정단위. 비우면 기존 값을 지웁니다.")
		String unit,

		@Schema(description = "측정분석방법. 비우면 기존 값을 지웁니다.")
		String analysisMethod,

		@Schema(description = "분석장비. 비우면 기존 값을 지웁니다.")
		String analysisEquipment
	) {}
}
