package com.ensolution.ems.schedule.application.service.assembler;

import com.ensolution.ems.auth.application.port.in.UserQueryUseCase;
import com.ensolution.ems.equipment.application.port.in.EquipmentQueryUseCase;
import com.ensolution.ems.equipment.application.port.in.EquipmentSummary;
import com.ensolution.ems.platform.application.port.in.TenantQueryUseCase;
import com.ensolution.ems.platform.application.port.in.TenantSummary;
import com.ensolution.ems.schedule.application.mapper.ScheduleSnapshotPortMapper;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.snapshot.EquipmentSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.SamplingItemSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.SamplingSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.TeamSnapshot;
import com.ensolution.ems.client_management.application.port.in.*;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 측정 시점 세부 스냅샷을 조립한다. 대상(측정시설)·팀·장비 정보를 공급 모듈(tenant·equipment)의
 * 인바운드 포트로 조회해 복사한다. 성적서 기본정보(관리번호·측정분야·일자)는 메타가 진실이므로
 * 스냅샷에 복사하지 않고, 내보내기 시점에 메타에서 직접 읽는다.
 * 단, 배출시설관리자·시료채취입회자는 사업장 원장의 값을 채취 스냅샷의 초기값으로 옮겨 담는다.
 */
@Component
@RequiredArgsConstructor
public class ScheduleSnapshotAssembler {

	private final StackQueryUseCase stackQueryUseCase;
	private final TeamQueryUseCase teamQueryUseCase;
	private final TenantQueryUseCase tenantQueryUseCase;
	private final EquipmentQueryUseCase equipmentQueryUseCase;
	private final UserQueryUseCase userQueryUseCase;
	private final ScheduleSnapshotPortMapper snapshotMapper;

	public ScheduleSnapshot assemble(Schedule meta, List<Long> pollutantIds) {
		Long tenantId = meta.getTenantId();

		StackMeasurementSummary stackSummary = stackQueryUseCase.getMeasurementTargetSummary(meta.getStackId(), tenantId);
		TeamSummary teamSummary = teamQueryUseCase.getTeamSummary(meta.getTeamId(), tenantId);
		TenantSummary tenantSummary = tenantQueryUseCase.getTenantSummary(tenantId);
		List<EquipmentSummary> equipmentSummaries = resolveEquipments(teamSummary, tenantId);

		List<StackMeasurementSummary.MeasurementItemInfo> selectedItems =
			filterByPollutantIds(stackSummary.measurementItems(), pollutantIds);

		StackMeasurementSummary.WorkplaceInfo workplace = stackSummary.workplace();
		SamplingSnapshot samplingData = SamplingSnapshot.create(
			workplace == null ? null : workplace.facilityManager(),
			workplace == null ? null : workplace.samplingWitness());

		return new ScheduleSnapshot(
			String.valueOf(meta.getId()),
			meta.getId(),
			tenantId,
			null,      // version은 저장 시 인프라(Spring Data)가 채운다.
			snapshotMapper.toClientSnapshot(stackSummary),
			snapshotMapper.toTenantSnapshot(tenantSummary),
			assembleTeam(meta, teamSummary, equipmentSummaries),
			samplingData,
			snapshotMapper.toItemSnapshots(selectedItems)
		);
	}

	/**
	 * 팀 스냅샷을 만들고 <b>이 회차의 측정자 표기</b>를 덮어쓴다.
	 * <p>
	 * 기본값은 팀 원장의 사수·부사수 이름이고, 계획에 따로 배정된 사람이 있으면 그 이름으로 바꾼다.
	 * 측정자는 팀 소속과 무관하게 테넌트 사용자 중에서 고르므로 이름은 팀이 아니라 auth 원장에서 온다.
	 * <p>
	 * <b>미배정(null)은 {@link TeamSnapshot#merge}가 기존 값을 유지해 그대로 팀 기본값이 남는다.</b>
	 * 이 폴백이 없으면 사수·부사수를 보내지 않던 기존 클라이언트의 성적서 표기가 빈칸이 된다.
	 * <p>
	 * 측정자 표기만 담은 patch를 넘긴다 — {@code teamId}·{@code teamName}·장비 목록은 방금 조립한
	 * 값이므로 건드리지 않는다({@code PATCH /{id}/team}이 표기를 고칠 때와 같은 경로다).
	 */
	private TeamSnapshot assembleTeam(Schedule meta, TeamSummary teamSummary, List<EquipmentSummary> equipments) {
		TeamSnapshot fromTeam = snapshotMapper.toTeamSnapshot(
			teamSummary, snapshotMapper.toEquipmentSnapshots(equipments));

		return fromTeam.merge(new TeamSnapshot(
			null, null,
			resolveMeasurerName(meta.getMentorId(), meta.getTenantId()),
			resolveMeasurerName(meta.getMenteeId(), meta.getTenantId()),
			null));
	}

	/**
	 * 배정된 측정자의 이름을 auth 원장에서 읽는다. 미배정이면 null을 돌려 팀 기본값을 유지하게 한다.
	 * <p>
	 * 존재·tenant 소속은 등록 시 {@code ScheduleValidator.requireMeasurersInTenant}가 이미 확인했다.
	 * 그럼에도 여기서 예외를 삼키지 않는 것은, 배정된 사용자가 지워진 채로 조립이 계속되면
	 * 팀 기본값이 그 사람 이름인 것처럼 성적서에 찍히기 때문이다.
	 */
	private String resolveMeasurerName(Long userId, Long tenantId) {
		return userId == null ? null : userQueryUseCase.getUser(userId, tenantId).name();
	}

	/**
	 * 시설의 측정항목 중 요청으로 선택된 측정물질(pollutantId)에 해당하는 항목만 남긴다.
	 * <p>
	 * <b>요청 순서를 그대로 따른다.</b> 측정항목의 순서는 성적서의 표기 순서이고, 템플릿이
	 * {@code ${items[0].name}} 처럼 인덱스로 칸을 지목하므로 순서가 결정적이어야 한다.
	 * 원장 조회 순서를 따르면 정렬이 보장되지 않고, 항목 교체 경로({@link #resolveItems})와도
	 * 규칙이 어긋난다. 선택된 물질이 원장에 없으면 조용히 빠진다(생성 시점의 기존 동작).
	 */
	private List<StackMeasurementSummary.MeasurementItemInfo> filterByPollutantIds(
		List<StackMeasurementSummary.MeasurementItemInfo> items, List<Long> pollutantIds) {
		Map<Long, StackMeasurementSummary.MeasurementItemInfo> available = items.stream()
			.collect(Collectors.toMap(
				StackMeasurementSummary.MeasurementItemInfo::pollutantId, Function.identity(), (a, b) -> a));

		return pollutantIds.stream()
			.filter(Objects::nonNull)
			.distinct()
			.map(available::get)
			.filter(Objects::nonNull)
			.toList();
	}

	/**
	 * 선택된 측정물질로 측정항목 스냅샷 목록을 다시 만든다(측정항목 교체 시 사용).
	 * <p>
	 * 이미 계획에 들어 있던 항목은 <b>기존 스냅샷 값을 그대로 유지</b>한다 — 측정 시점의 허용기준으로
	 * 이미 시트를 채웠을 수 있어, 원장이 그 뒤 바뀌었다고 덮어쓰면 기록이 흔들린다.
	 * 새로 추가된 항목만 측정시설 원장에서 조회해 조립하며, 원장에 없는 물질은 거부한다.
	 */
	public List<SamplingItemSnapshot> resolveItems(
		Schedule meta, List<Long> pollutantIds, List<SamplingItemSnapshot> current) {

		Map<Long, SamplingItemSnapshot> kept = current == null ? Map.of() : current.stream()
			.collect(Collectors.toMap(SamplingItemSnapshot::pollutantId, Function.identity(), (a, b) -> a));

		StackMeasurementSummary stackSummary =
			stackQueryUseCase.getMeasurementTargetSummary(meta.getStackId(), meta.getTenantId());
		Map<Long, StackMeasurementSummary.MeasurementItemInfo> available = stackSummary.measurementItems().stream()
			.collect(Collectors.toMap(
				StackMeasurementSummary.MeasurementItemInfo::pollutantId, Function.identity(), (a, b) -> a));

		return pollutantIds.stream()
			.filter(Objects::nonNull)
			.distinct()
			.map(pollutantId -> {
				SamplingItemSnapshot existing = kept.get(pollutantId);
				if (existing != null) return existing;

				StackMeasurementSummary.MeasurementItemInfo info = available.get(pollutantId);
				if (info == null) throw new CustomException(ErrorCode.SCHEDULE_ITEM_NOT_IN_STACK);
				return snapshotMapper.toItemSnapshot(info);
			})
			.toList();
	}

	/**
	 * 전달된 장비 id로 장비 스냅샷 목록을 조회한다(장비 교체 시 사용).
	 * 유형별 슬롯이 아니라 목록이다 — 어떤 유형인지는 장비 원장이 알고 있다.
	 */
	public List<EquipmentSnapshot> resolveEquipments(List<String> equipmentIds, Long tenantId) {
		return snapshotMapper.toEquipmentSnapshots(fetchEquipments(
			equipmentIds == null ? Stream.of() : equipmentIds.stream(), tenantId));
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