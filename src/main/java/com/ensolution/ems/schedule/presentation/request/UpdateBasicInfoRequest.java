package com.ensolution.ems.schedule.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "측정계획 기본 정보 수정 요청. 전달하지 않은 필드는 기존 값을 유지합니다. "
	+ "측정분야·채취일자·측정용도·내부 식별 코드는 PUT /api/schedules/{scheduleId} 로 수정합니다.")
public record UpdateBasicInfoRequest(
	@Schema(description = "배출시설관리자", example = "김담당")
	String facilityManager,

	@Schema(description = "시료채취입회자(환경기술인)", example = "이입회")
	String samplingWitness,

	@Schema(description = "시료분석검사자", example = "박분석")
	String analyst,

	@Schema(description = "기술책임자", example = "최기술")
	String technicalManager,

	@Schema(description = "시료접수일자", example = "2026-05-02")
	LocalDate receivedAt,

	@Schema(description = "분석완료일자", example = "2026-05-04")
	LocalDate analyzedAt,

	@Schema(description = "성적서발행일자", example = "2026-05-08")
	LocalDate issuedAt,

	@Schema(description = "채취시작시간", example = "09:30")
	LocalTime samplingStartedAt,

	@Schema(description = "채취종료시간", example = "11:00")
	LocalTime samplingEndedAt,

	@Schema(description = "측정자(멘토) 표기명. 팀 원장은 변경하지 않습니다.", example = "홍길동")
	String mentorName,

	@Schema(description = "측정자(멘티) 표기명. 팀 원장은 변경하지 않습니다.", example = "김철수")
	String menteeName
) {}
