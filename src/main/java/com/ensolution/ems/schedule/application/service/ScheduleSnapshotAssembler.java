package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.equipment.application.port.in.EquipmentQueryUseCase;
import com.ensolution.ems.equipment.application.port.in.EquipmentSummary;
import com.ensolution.ems.platform.application.port.in.TenantQueryUseCase;
import com.ensolution.ems.platform.application.port.in.TenantSummary;
import com.ensolution.ems.schedule.application.mapper.ScheduleSnapshotMapper;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.snapshot.BasicInfo;
import com.ensolution.ems.schedule.domain.snapshot.EquipmentSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.TeamSnapshot;
import com.ensolution.ems.tenant.application.port.in.*;
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
 * 단, 배출시설관리자·시료채취입회자는 사업장 원장의 값을 기본 정보의 초기값으로 옮겨 담는다.
 */
@Component
@RequiredArgsConstructor
public class ScheduleSnapshotAssembler {

	private final StackQueryUseCase stackQueryUseCase;
	private final TeamQueryUseCase teamQueryUseCase;
	private final TenantQueryUseCase tenantQueryUseCase;
	private final EquipmentQueryUseCase equipmentQueryUseCase;
	private final ScheduleSnapshotMapper snapshotMapper;

	public ScheduleSnapshot assemble(Schedule meta, List<Long> pollutantIds) {
		Long tenantId = meta.getTenantId();

		StackMeasurementSummary stackSummary = stackQueryUseCase.getMeasurementTargetSummary(meta.getStackId(), tenantId);
		TeamSummary teamSummary = teamQueryUseCase.getTeamSummary(meta.getTeamId(), tenantId);
		TenantSummary tenantSummary = tenantQueryUseCase.getTenantSummary(tenantId);
		List<EquipmentSummary> equipmentSummaries = resolveEquipments(teamSummary, tenantId);

		List<StackMeasurementSummary.MeasurementItemInfo> selectedItems =
			filterByPollutantIds(stackSummary.measurementItems(), pollutantIds);

		StackMeasurementSummary.WorkplaceInfo workplace = stackSummary.workplace();
		BasicInfo basicInfo = BasicInfo.create(
			meta.getReferenceNumber(),
			meta.getSampledAt(),
			meta.getMeasurementField(),
			meta.getSchedulePurpose(),
			workplace == null ? null : workplace.facilityManager(),
			workplace == null ? null : workplace.samplingWitness());

		return new ScheduleSnapshot(
			String.valueOf(meta.getId()),
			meta.getId(),
			tenantId,
			meta.getStatus(),
			basicInfo,
			snapshotMapper.toTeamSnapshot(teamSummary),
			snapshotMapper.toTenantSnapshot(tenantSummary),
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

	/** 팀 스냅샷의 장비 id 4종으로 장비 스냅샷 목록을 다시 조회한다(장비 교체 시 사용). */
	public List<EquipmentSnapshot> resolveEquipments(TeamSnapshot team, Long tenantId) {
		return snapshotMapper.toEquipmentSnapshots(fetchEquipments(
			Stream.of(
				team.particleSamplerId(),
				team.gasSamplerId(),
				team.pitotTubeId(),
				team.nozzleId()
			), tenantId));
	}

	private List<EquipmentSummary> resolveEquipments(TeamSummary team, Long tenantId) {
		return fetchEquipments(
			Stream.of(
				team.particleSamplerId(),
				team.gasSamplerId(),
				team.pitotTubeId(),
				team.nozzleId()
			), tenantId);
	}

	/** 비어 있지 않은 장비 id만 골라 equipment 모듈에서 요약 정보를 조회한다. */
	private List<EquipmentSummary> fetchEquipments(Stream<String> equipmentIds, Long tenantId) {
		return equipmentIds
			.filter(Objects::nonNull)
			.filter(id -> !id.isBlank())
			.map(id -> equipmentQueryUseCase.getEquipmentSummary(id, tenantId))
			.toList();
	}
}
