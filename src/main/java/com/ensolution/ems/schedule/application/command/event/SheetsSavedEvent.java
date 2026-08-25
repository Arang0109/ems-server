package com.ensolution.ems.schedule.application.command.event;

import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.domain.sheet.MeasurementCategory;

import java.util.List;

/**
 * 측정 시트가 저장됐음을 같은 측정계획을 열어둔 다른 사용자에게 알리는 이벤트.
 * <p>
 * 시트 본문을 싣지 않고 "무엇이 바뀌었는지"만 알린다 — 수신 측은 이 알림을 받고
 * {@code GET /api/schedules/{id}}로 최신본을 조회한다. 페이로드가 작고, 기존 응답 DTO를
 * 그대로 재사용하며, 알림이 유실되더라도 조회 결과가 늘 진실의 원천으로 남는다.
 */
public record SheetsSavedEvent(
	Long scheduleId,
	Long tenantId,
	EditorRef editor,
	List<MeasurementCategory> categories,
	ScheduleStatus status
) {}
