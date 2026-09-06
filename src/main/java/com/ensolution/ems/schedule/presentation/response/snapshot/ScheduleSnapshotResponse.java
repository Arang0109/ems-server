package com.ensolution.ems.schedule.presentation.response.snapshot;

import java.util.List;

/**
 * 측정계획 세부 스냅샷 응답. 측정 시점의 대상·팀·장비·측정항목 사본과 채취 정보를 담는다.
 * <p>
 * <b>메타와 겹치는 값은 담지 않는다</b> — 관리번호·측정분야·측정용도와 채취일자·시료접수일·
 * 분석완료일·성적서발행일, 그리고 상태는 응답 최상위({@code ScheduleResponse})에 있고 그쪽이
 * 진실의 원천이다(메타는 MySQL, 문서는 사본). 문서에 사본을 두지 않으므로 두 값이 어긋날 일도 없다.
 * <p>
 * <b>문서의 저장 메타도 담지 않는다</b> — {@code id}(Mongo _id)·{@code scheduleId}·{@code tenantId}는
 * 최상위에 있고, {@code version}(문서 단위 낙관적 락)과 문서 생성 시각은 서버 내부 값이다.
 * <p>
 * 장비는 팀 아래({@code team.equipments}), 채취 기록지는 채취 정보 아래
 * ({@code samplingData.sheets})에 있다. 실험분석 결과는 측정항목 안({@code items[].analysis})에 있다.
 */
public record ScheduleSnapshotResponse(
	TeamSnapshotResponse team,
	TenantSnapshotResponse tenant,
	ClientSnapshotResponse client,
	SamplingSnapshotResponse samplingData,
	List<SamplingItemSnapshotResponse> items
) {}
