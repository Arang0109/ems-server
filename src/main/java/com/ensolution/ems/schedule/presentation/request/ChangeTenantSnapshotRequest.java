package com.ensolution.ems.schedule.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "고객사 스냅샷 수정 요청. 전달하지 않은(공백 포함) 필드는 기존 값을 유지합니다. "
	+ "이 회차 문서만 고치며 고객사 원장은 바뀌지 않습니다.")
public record ChangeTenantSnapshotRequest(
	@Schema(description = "고객사명")
	String name,

	@Schema(description = "사업자등록번호")
	String bizNumber,

	@Schema(description = "대표자")
	String representative,

	@Schema(description = "도로명주소")
	String roadAddress,

	@Schema(description = "상세주소")
	String detailAddress,

	@Schema(description = "우편번호")
	String zipcode,

	@Schema(description = "시료분석검사자. 성적서 서명란에 들어갑니다.")
	String analyst,

	@Schema(description = "기술책임자. 성적서 서명란에 들어갑니다.")
	String technicalManager
) {}
