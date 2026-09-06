package com.ensolution.ems.schedule.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 성적서를 진행하며 채우는 값의 수정 요청. 값의 주인이 넷으로 갈려 있어 서버가 나눠 저장한다
 * (일자 셋은 측정계획 메타, 채취 시각·현장 담당자는 채취 스냅샷, 서명란 담당자는 고객사 스냅샷,
 * 측정자 표기는 팀 스냅샷). 원장은 어느 것도 바뀌지 않는다.
 *
 * <p>현장 채취 화면과 실험·분석 화면이 이 경로를 공유하며 각자 자기 것이 아닌 칸에 null을 실어
 * 보내므로 <b>전부 부분 갱신</b>이다 — null은 "미전달"이지 "지움"이 아니다.
 */
@Schema(description = "성적서를 진행하며 채우는 값의 수정 요청. 여러 화면이 공유하는 경로라 "
	+ "전달하지 않은(공백 포함) 값은 기존 값을 유지합니다(부분 갱신). 그래서 이미 채운 값을 이 경로로 비울 수는 없습니다. "
	+ "채취일자·측정용도·관리번호는 PUT /api/schedules/{scheduleId} 로 수정하며, "
	+ "측정분야는 생성 시점에만 정합니다.")
public record UpdateBasicInfoRequest(
	@Schema(description = "시료접수일자", example = "2026-05-02")
	LocalDate receivedAt,

	@Schema(description = "분석완료일자", example = "2026-05-04")
	LocalDate analyzedAt,

	@Schema(description = "성적서발행일자", example = "2026-05-08")
	LocalDate issuedAt,

	@Schema(description = "채취시작시각", example = "09:30")
	LocalTime samplingStartedAt,

	@Schema(description = "채취종료시각", example = "11:00")
	LocalTime samplingEndedAt,

	@Schema(description = "배출시설관리자")
	String facilityManager,

	@Schema(description = "시료채취입회자(환경기술인)")
	String samplingWitness,

	@Schema(description = "시료분석검사자. 고객사 기본값과 다르면 이 회차만 바뀝니다.")
	String analyst,

	@Schema(description = "기술책임자. 고객사 기본값과 다르면 이 회차만 바뀝니다.")
	String technicalManager,

	@Schema(description = "측정자(사수) 표기명. 팀 원장은 바뀌지 않습니다.")
	String mentorName,

	@Schema(description = "측정자(부사수) 표기명. 팀 원장은 바뀌지 않습니다.")
	String menteeName
) {}
