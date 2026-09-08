package com.ensolution.ems.schedule.presentation.response;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.presentation.response.snapshot.ScheduleSnapshotResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 측정계획 상세 응답. 메타데이터와 측정 시점 세부 스냅샷 트리를 함께 노출한다.
 *
 * <p><b>성적서 기본정보는 전부 최상위에 있다.</b> 관리번호·측정분야·측정용도와 채취일자·시료접수일·
 * 분석완료일·성적서발행일은 메타(MySQL)가 진실의 원천이며, 문서에 사본을 두지 않으므로 두 곳이
 * 어긋날 일이 없다. {@code snapshot}은 이 값들을 담지 않는다({@link ScheduleSnapshotResponse}).
 *
 * <p>일자 넷은 수정 경로가 갈린다 — 채취일자는 계획을 정의하는 값이라
 * {@code PUT /api/schedules/{id}}, 나머지 셋은 진행하며 채우는 값이라
 * {@code PATCH /api/schedules/{id}/basic-info}가 맡는다.
 *
 * <p><b>{@code mentorId}·{@code menteeId}는 배정 사실이고, 성적서에 인쇄되는 이름은
 * {@code snapshot.team.mentorName}·{@code menteeName}이다.</b> 등록 화면이 선택지를 되채우려면 id가
 * 필요하고, 표기는 이후 자유 편집되므로 둘은 갈릴 수 있다 — {@code teamId}와 {@code team.teamName}의
 * 관계와 같아서 위의 "사본을 두지 않는다"에 어긋나지 않는다.
 */
public record ScheduleResponse(
	Long id,
	Long tenantId,
	Long stackId,
	Long teamId,
	Long mentorId,
	Long menteeId,
	MeasurementField measurementField,
	LocalDate sampledAt,
	LocalDate receivedAt,
	LocalDate analyzedAt,
	LocalDate issuedAt,
	String schedulePurpose,
	ScheduleStatus status,
	String referenceNumber,
	LocalDateTime createdAt,
	LocalDateTime modifiedAt,
	ScheduleSnapshotResponse snapshot
) {}
