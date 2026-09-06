package com.ensolution.ems.schedule.application.mapper;

import com.ensolution.ems.equipment.application.port.in.EquipmentSummary;
import com.ensolution.ems.platform.application.port.in.TenantSummary;
import com.ensolution.ems.schedule.domain.snapshot.*;
import com.ensolution.ems.client_management.application.port.in.StackMeasurementSummary;
import com.ensolution.ems.client_management.application.port.in.TeamSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 인터모듈 매퍼: 공급 모듈(tenant·equipment)의 {@code port/in} 요약 VO를 schedule 도메인 스냅샷으로 변환한다.
 * 리프 변환은 MapStruct가 생성하고, 의뢰기관→사업장→측정시설 트리 조립은 default 메서드로 구성한다.
 */
@Mapper(componentModel = "spring")
public interface ScheduleSnapshotPortMapper {
	
	TenantSnapshot toTenantSnapshot(TenantSummary summary);

	/**
	 * 팀 스냅샷을 만든다. 장비는 팀 원장이 아니라 장비 원장에서 따로 조회해 오므로 인자로 받는다 —
	 * 팀 원장은 유형별 슬롯으로 id만 갖고 있고, 스냅샷은 그것을 실제 장비 사본의 목록으로 물질화한다.
	 */
	@Mapping(target = "equipments", source = "equipments")
	@Mapping(target = "withEquipments", ignore = true)   // 도메인 파생 메서드일 뿐 property가 아니다
	TeamSnapshot toTeamSnapshot(TeamSummary summary, List<EquipmentSnapshot> equipments);

	@Mapping(target = "equipmentId", source = "id")
	EquipmentSnapshot toEquipmentSnapshot(EquipmentSummary summary);

	List<EquipmentSnapshot> toEquipmentSnapshots(List<EquipmentSummary> summaries);

	// 새로 조립되는 항목은 아직 분석 전이므로 analysis가 null이다(비어 있는 것과 뜻이 다르다).
	@Mapping(target = "analysis", ignore = true)
	@Mapping(target = "withAnalysis", ignore = true)     // 도메인 파생 메서드일 뿐 property가 아니다
	SamplingItemSnapshot toItemSnapshot(StackMeasurementSummary.MeasurementItemInfo info);

	List<SamplingItemSnapshot> toItemSnapshots(List<StackMeasurementSummary.MeasurementItemInfo> infos);

	FacilitySnapshot toFacilitySnapshot(StackMeasurementSummary.FacilityInfo info);

	List<FacilitySnapshot> toFacilitySnapshots(List<StackMeasurementSummary.FacilityInfo> infos);

	PreventionSnapshot toPreventionSnapshot(StackMeasurementSummary.PreventionInfo info);

	List<PreventionSnapshot> toPreventionSnapshots(List<StackMeasurementSummary.PreventionInfo> infos);

	/**
	 * 의뢰기관 스냅샷 트리(→ 사업장 → 측정시설)를 조립한다.
	 * 담당자(의뢰기관의 {@code manager}, 사업장의 {@code facilityManager}·{@code samplingWitness})는
	 * 측정계획마다 달라지는 값이므로 이 트리에 담지 않는다.
	 * 사업장 원장의 두 담당자는 {@code ScheduleSnapshotAssembler}에서 채취 스냅샷의 초기값으로 옮겨 담는다.
	 */
	default ClientSnapshot toClientSnapshot(StackMeasurementSummary summary) {
		if (summary == null || summary.client() == null) return null;
		StackMeasurementSummary.ClientInfo c = summary.client();
		return new ClientSnapshot(
			c.clientId(), c.name(), c.bizNumber(), c.representative(),
			c.roadAddress(), c.detailAddress(), c.zipcode(), c.email(), c.tel(),
			toWorkplaceSnapshot(summary)
		);
	}

	default WorkplaceSnapshot toWorkplaceSnapshot(StackMeasurementSummary summary) {
		StackMeasurementSummary.WorkplaceInfo w = summary.workplace();
		if (w == null) return null;
		return new WorkplaceSnapshot(
			w.workplaceId(), w.name(), w.bizNumber(), w.businessCategory(),
			w.roadAddress(), w.detailAddress(), w.zipcode(), w.grade(),
			toStackSnapshot(summary)
		);
	}

	default StackSnapshot toStackSnapshot(StackMeasurementSummary summary) {
		StackMeasurementSummary.StackInfo s = summary.stack();
		if (s == null) return null;
		return new StackSnapshot(
			s.stackId(), s.field(), s.name(), s.semsNumber(), s.grade(),
			s.mainProduct(), s.standardOxygen(),
			s.height(), s.horizontalLength(), s.verticalLength(),
			s.shape(), s.orientation(),
			toFacilitySnapshots(summary.facilities()),
			toPreventionSnapshots(summary.preventions())
		);
	}
}
