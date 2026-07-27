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
	ScheduleStatus status,
	BasicInfo basicInfo,
	TeamSnapshot team,
	TenantSnapshot tenant,
	ClientSnapshot client,
	List<EquipmentSnapshot> equipments,
	List<SamplingItemSnapshot> items,
	List<MeasurementSheet> sheets
) {
	/** 상태만 교체한 새 스냅샷을 반환한다(메타 상태와 동기화용). */
	public ScheduleSnapshot syncStatus(ScheduleStatus next) {
		return new ScheduleSnapshot(id, scheduleId, tenantId, next, basicInfo, team, tenant, client, equipments, items, sheets);
	}

	/** 측정 시트만 교체한 새 스냅샷을 반환한다. */
	public ScheduleSnapshot withSheets(List<MeasurementSheet> newSheets) {
		return new ScheduleSnapshot(id, scheduleId, tenantId, status, basicInfo, team, tenant, client, equipments, items, newSheets);
	}

	/**
	 * 측정장비 교체 결과를 반영한 새 스냅샷을 반환한다. 팀의 장비 id(team)·장비 목록(equipments)·
	 * 재계산된 시트(sheets)를 함께 교체하며, 원장(tenant·equipment)은 변경하지 않는다.
	 */
	public ScheduleSnapshot applyEquipmentChange(TeamSnapshot newTeam,
	                                             List<EquipmentSnapshot> newEquipments,
	                                             List<MeasurementSheet> newSheets) {
		return new ScheduleSnapshot(id, scheduleId, tenantId, status, basicInfo,
			newTeam, tenant, client, newEquipments, items, newSheets);
	}

	/**
	 * 의뢰기관 스냅샷 수정 결과를 반영한 새 스냅샷을 반환한다. client 트리(→사업장→측정시설)를 재귀 병합하고
	 * 재계산된 시트를 함께 교체하며, 원장(tenant)은 변경하지 않는다.
	 */
	public ScheduleSnapshot applyClientChange(ClientSnapshot patch, List<MeasurementSheet> newSheets) {
		return new ScheduleSnapshot(id, scheduleId, tenantId, status, basicInfo, team, tenant,
			client == null ? patch : client.merge(patch),
			equipments, items, newSheets);
	}

	/**
	 * 기본 정보(담당자·일자·채취 시각)와 측정자 표기 수정 결과를 반영한 새 스냅샷을 반환한다.
	 * 계산 입력이 아닌 값만 다루므로 측정 시트는 재계산하지 않고 그대로 유지하며, 원장(tenant·client)도 변경하지 않는다.
	 */
	public ScheduleSnapshot applyBasicInfo(BasicInfo newBasicInfo, TeamSnapshot newTeam) {
		return new ScheduleSnapshot(id, scheduleId, tenantId, status, newBasicInfo,
			newTeam, tenant, client, equipments, items, sheets);
	}

	/**
	 * 메타 수정 결과를 문서에 반영한다. basicInfo·referenceNumber를 갱신하며, client 트리는 변경하지 않는다
	 * (의뢰기관·사업장·측정시설 스냅샷 수정은 {@link #applyClientChange} 경로를 사용한다).
	 */
	public ScheduleSnapshot applyMetaUpdate(BasicInfo newBasicInfo, TenantSnapshot newTenant) {
		return new ScheduleSnapshot(
			id, scheduleId, tenantId,
			status, newBasicInfo, team, newTenant,
			client,
			equipments, items, sheets);
	}
}