package com.ensolution.ems.schedule.application.mapper;

import com.ensolution.ems.equipment.application.port.in.EquipmentSummary;
import com.ensolution.ems.schedule.domain.snapshot.ClientSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.EquipmentSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.FacilitySnapshot;
import com.ensolution.ems.schedule.domain.snapshot.MeasurementItemSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.PreventionSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.StackSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.TargetSubstanceSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.TeamSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.WorkplaceSnapshot;
import com.ensolution.ems.tenant.application.port.in.StackMeasurementSummary;
import com.ensolution.ems.tenant.application.port.in.TeamSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 인터모듈 매퍼: 공급 모듈(tenant·equipment)의 {@code port/in} 요약 VO를 schedule 도메인 스냅샷으로 변환한다.
 * 리프 변환은 MapStruct가 생성하고, 의뢰기관→사업장→측정시설 트리 조립은 default 메서드로 구성한다.
 */
@Mapper(componentModel = "spring")
public interface ScheduleSnapshotMapper {

	TeamSnapshot toTeamSnapshot(TeamSummary summary);

	@Mapping(target = "equipmentId", source = "id")
	EquipmentSnapshot toEquipmentSnapshot(EquipmentSummary summary);

	List<EquipmentSnapshot> toEquipmentSnapshots(List<EquipmentSummary> summaries);

	MeasurementItemSnapshot toItemSnapshot(StackMeasurementSummary.MeasurementItemInfo info);

	List<MeasurementItemSnapshot> toItemSnapshots(List<StackMeasurementSummary.MeasurementItemInfo> infos);

	FacilitySnapshot toFacilitySnapshot(StackMeasurementSummary.FacilityInfo info);

	List<FacilitySnapshot> toFacilitySnapshots(List<StackMeasurementSummary.FacilityInfo> infos);

	TargetSubstanceSnapshot toTargetSubstanceSnapshot(StackMeasurementSummary.TargetSubstanceInfo info);

	PreventionSnapshot toPreventionSnapshot(StackMeasurementSummary.PreventionInfo info);

	List<PreventionSnapshot> toPreventionSnapshots(List<StackMeasurementSummary.PreventionInfo> infos);

	/** 의뢰기관 스냅샷 트리(→ 사업장 → 측정시설)를 조립한다. */
	default ClientSnapshot toClientSnapshot(StackMeasurementSummary summary) {
		if (summary == null || summary.client() == null) return null;
		StackMeasurementSummary.ClientInfo c = summary.client();
		return new ClientSnapshot(
			c.clientId(), c.name(), c.bizNumber(), c.representative(),
			c.roadAddress(), c.detailAddress(), c.zipcode(), c.manager(), c.email(), c.tel(),
			toWorkplaceSnapshot(summary)
		);
	}

	default WorkplaceSnapshot toWorkplaceSnapshot(StackMeasurementSummary summary) {
		StackMeasurementSummary.WorkplaceInfo w = summary.workplace();
		if (w == null) return null;
		return new WorkplaceSnapshot(
			w.workplaceId(), w.name(), w.bizNumber(),
			w.roadAddress(), w.detailAddress(), w.zipcode(), w.grade(),
			toStackSnapshot(summary)
		);
	}

	default StackSnapshot toStackSnapshot(StackMeasurementSummary summary) {
		StackMeasurementSummary.StackInfo s = summary.stack();
		if (s == null) return null;
		return new StackSnapshot(
			s.stackId(), s.field(), s.name(), s.semsNumber(), s.grade(),
			s.businessCategory(), s.mainProduct(), s.standardOxygen(), s.height(), s.horizontalLength(), s.verticalLength(),
			s.shape(), s.orientation(),
			toFacilitySnapshots(summary.facilities()),
			toPreventionSnapshots(summary.preventions())
		);
	}
}
