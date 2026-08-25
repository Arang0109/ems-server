package com.ensolution.ems.schedule.presentation.response.snapshot;

import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;

import java.util.List;

/**
 * 측정계획 세부 스냅샷 응답. 측정 시점의 대상·팀·장비·측정항목 사본과 측정 시트를 담는다.
 * <p>
 * <b>문서의 저장 메타는 담지 않는다</b> — {@code id}(Mongo _id)·{@code scheduleId}·{@code tenantId}·
 * {@code status}·{@code version}·{@code createdAt}은 응답에서 제외한다. 앞의 넷은 응답 최상위
 * ({@code ScheduleResponse})에 이미 있고 그쪽이 진실의 원천이며(메타는 MySQL, 문서는 사본),
 * 뒤의 둘은 문서 단위 낙관적 락 토큰과 문서 생성 시각이라 서버 내부 값이다.
 * <p>
 * 다만 {@code sheets[].version}은 <b>반드시 유지된다</b> — 클라이언트가 읽어간 시트 버전을 그대로
 * 되돌려 보내야 동시 편집 충돌을 판정할 수 있다({@code docs/DATABASE.md}).
 * <p>
 * {@code sheets}가 도메인 타입인 이유는 {@code SaveSheetsRequest}와 같다 — 시트는 클라이언트가
 * 읽어서 그대로 되돌려 보내는 왕복 페이로드라, 응답만 감싸면 요청과 모양이 갈라진다.
 */
public record ScheduleSnapshotResponse(
	BasicInfoResponse basicInfo,
	TeamSnapshotResponse team,
	TenantSnapshotResponse tenant,
	ClientSnapshotResponse client,
	List<EquipmentSnapshotResponse> equipments,
	List<SamplingItemSnapshotResponse> items,
	List<MeasurementSheet> sheets
) {}
