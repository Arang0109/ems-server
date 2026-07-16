package com.ensolution.ems.schedule.domain.snapshot;

import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;

import java.util.List;

/**
 * 측정계획 세부 스냅샷 애그리거트(MongoDB 문서 본문의 도메인 표현).
 * 측정 시점의 대상·팀·장비·측정항목 정보를 복사해 불변으로 보관하므로,
 * 원장(tenant·equipment)이 변경되어도 과거 기록은 영향을 받지 않는다.
 * {@code sheets}는 실제 측정값과 계산 결과를 담는 측정 시트 목록이다.
 */
public record ScheduleSnapshot(
	String id,             // Mongo _id (= scheduleId 문자열)
	Long scheduleId,       // MySQL 메타 PK 연결키
	Long tenantId,
	String referenceNumber,
	ScheduleStatus status,
	BasicInfo basicInfo,
	TeamSnapshot team,
	ClientSnapshot client,
	List<EquipmentSnapshot> equipments,
	List<MeasurementItemSnapshot> items,
	List<MeasurementSheet> sheets
) {
	/** 상태만 교체한 새 스냅샷을 반환한다(메타 상태와 동기화용). */
	public ScheduleSnapshot syncStatus(ScheduleStatus next) {
		return new ScheduleSnapshot(id, scheduleId, tenantId, referenceNumber, next, basicInfo, team, client, equipments, items, sheets);
	}

	/** 측정 시트만 교체한 새 스냅샷을 반환한다. */
	public ScheduleSnapshot withSheets(List<MeasurementSheet> newSheets) {
		return new ScheduleSnapshot(id, scheduleId, tenantId, referenceNumber, status, basicInfo, team, client, equipments, items, newSheets);
	}

	/**
	 * 메타 수정 결과를 문서에 반영한다. basicInfo·referenceNumber는 항상 갱신하고,
	 * client 트리는 전달된 경우에만 전체 교체(overwrite)하며 null이면 기존 트리를 유지한다.
	 */
	public ScheduleSnapshot applyMetaUpdate(BasicInfo newBasicInfo, ClientSnapshot newClient) {
		return new ScheduleSnapshot(
			id, scheduleId, tenantId,
			newBasicInfo.referenceNumber(),
			status, newBasicInfo, team,
			newClient != null ? newClient : client,
			equipments, items, sheets);
	}
}