package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.equipment.application.port.in.EquipmentQueryUseCase;
import com.ensolution.ems.equipment.application.port.in.EquipmentSummary;
import com.ensolution.ems.schedule.application.mapper.ScheduleSnapshotMapper;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.snapshot.BasicInfo;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import com.ensolution.ems.tenant.application.port.in.StackMeasurementSummary;
import com.ensolution.ems.tenant.application.port.in.StackQueryUseCase;
import com.ensolution.ems.tenant.application.port.in.TeamQueryUseCase;
import com.ensolution.ems.tenant.application.port.in.TeamSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 측정 시점 세부 스냅샷을 조립한다. 대상(측정시설)·팀·장비 정보를 공급 모듈(tenant·equipment)의
 * 인바운드 포트로 조회해 복사하며, 기본 정보는 메타(Schedule)에서 가져온다.
 */
@Component
@RequiredArgsConstructor
public class ScheduleSnapshotAssembler {

	private final StackQueryUseCase stackQueryUseCase;
	private final TeamQueryUseCase teamQueryUseCase;
	private final EquipmentQueryUseCase equipmentQueryUseCase;
	private final ScheduleSnapshotMapper snapshotMapper;

	public ScheduleSnapshot assemble(Schedule meta, List<Long> pollutantIds) {
		Long tenantId = meta.getTenantId();

		StackMeasurementSummary stackSummary = stackQueryUseCase.getMeasurementTargetSummary(meta.getStackId(), tenantId);
		TeamSummary teamSummary = teamQueryUseCase.getTeamSummary(meta.getTeamId(), tenantId);
		List<EquipmentSummary> equipmentSummaries = resolveEquipments(teamSummary, tenantId);

		List<StackMeasurementSummary.MeasurementItemInfo> selectedItems =
			filterByPollutantIds(stackSummary.measurementItems(), pollutantIds);

		BasicInfo basicInfo = new BasicInfo(
			meta.getReferenceNumber(),
			meta.getMeasureDate(),
			meta.getMeasurementField(),
			meta.getMeasurementType()
		);

		return new ScheduleSnapshot(
			String.valueOf(meta.getId()),
			meta.getId(),
			tenantId,
			meta.getReferenceNumber(),
			meta.getStatus(),
			basicInfo,
			snapshotMapper.toTeamSnapshot(teamSummary),
			snapshotMapper.toClientSnapshot(stackSummary),
			snapshotMapper.toEquipmentSnapshots(equipmentSummaries),
			snapshotMapper.toItemSnapshots(selectedItems),
			List.of()
		);
	}

	/** 시설의 측정항목 중 요청으로 선택된 측정물질(pollutantId)에 해당하는 항목만 남긴다. */
	private List<StackMeasurementSummary.MeasurementItemInfo> filterByPollutantIds(
		List<StackMeasurementSummary.MeasurementItemInfo> items, List<Long> pollutantIds) {
		Set<Long> selected = new HashSet<>(pollutantIds);
		return items.stream()
			.filter(item -> selected.contains(item.pollutantId()))
			.toList();
	}

	private List<EquipmentSummary> resolveEquipments(TeamSummary team, Long tenantId) {
		return Stream.of(
				team.particleSamplerId(),
				team.gasSamplerId(),
				team.pitotTubeId(),
				team.nozzleId()
			)
			.filter(Objects::nonNull)
			.filter(id -> !id.isBlank())
			.map(id -> equipmentQueryUseCase.getEquipmentSummary(id, tenantId))
			.toList();
	}
}
