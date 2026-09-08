package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.command.detail.ScheduleDetail;
import com.ensolution.ems.schedule.application.command.update.ChangeClientSnapshotCommand;
import com.ensolution.ems.schedule.application.command.update.ChangeScheduleEquipmentsCommand;
import com.ensolution.ems.schedule.application.command.update.UpdateBasicInfoCommand;
import com.ensolution.ems.schedule.application.command.update.UpdateScheduleItemCommand;
import com.ensolution.ems.schedule.application.port.out.ScheduleRepository;
import com.ensolution.ems.schedule.application.service.assembler.ScheduleSnapshotAssembler;
import com.ensolution.ems.schedule.application.service.support.ScheduleStatusTransitioner;
import com.ensolution.ems.schedule.application.service.support.SnapshotSheetReCalculator;
import com.ensolution.ems.schedule.application.service.support.SnapshotWriter;
import com.ensolution.ems.schedule.application.validator.ScheduleValidator;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.ScheduleProgress;
import com.ensolution.ems.schedule.domain.snapshot.ClientSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.EquipmentSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.SamplingSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 측정 시점 스냅샷(MongoDB 문서) 편집 유스케이스.
 *
 * <p>수정 경로가 여섯인 것은 <b>의도된 설계</b>다. 경로마다 재계산 여부·null 시맨틱·부작용 범위가
 * 달라, 하나로 합치면 그 차이가 전부 서비스 내부 조건문으로 이동한다. 근거는
 * {@code schedule/.claude/CLAUDE.md}의 "수정 경로 규약"에 있다.
 *
 * <p>공통 규약 셋 — ① <b>원장을 절대 건드리지 않는다</b>({@code client_management}·{@code equipment}·
 * {@code platform}). 원장까지 고쳐야 하면 호출자가 원장 API를 따로 호출한다. ② 모든 경로가
 * {@link Schedule#requireEditable()}을 지난다. ③ 문서 쓰기는 예외 없이 {@link SnapshotWriter}를
 * 지나 낙관적 락 재시도를 탄다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleSnapshotService {

	private final ScheduleRepository scheduleRepository;
	private final ScheduleSnapshotAssembler snapshotAssembler;
	private final SnapshotSheetReCalculator reCalculator;
	private final SnapshotWriter snapshotWriter;
	private final ScheduleValidator scheduleValidator;
	private final ScheduleStatusTransitioner statusTransitioner;

	/**
	 * 측정계획의 측정장비를 교체한다. 전달된 목록으로 <b>전체 교체</b>하며(부분 갱신이 아니다),
	 * 장비 유형은 장비 원장이 알고 있으므로 요청이 슬롯을 지정하지 않는다.
	 * 기존 시트는 새 장비 spec으로 재계산한다.
	 * 장비는 메타(MySQL)가 아닌 문서(MongoDB)에만 존재하므로 상태 전이가 없는 한 문서 단독 쓰기이며,
	 * 원장(tenant·equipment)은 변경하지 않는다. 완료·취소된 계획은 변경할 수 없다.
	 */
	public ScheduleDetail changeEquipments(Long id, Long tenantId, ChangeScheduleEquipmentsCommand command) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		meta.requireEditable();

		// 장비 원장 조회는 변경 함수 밖에서 끝낸다 — 재시도로 여러 번 호출되는 자리이기 때문이다.
		List<EquipmentSnapshot> equipments =
			snapshotAssembler.resolveEquipments(command.equipmentIds(), tenantId);

		ScheduleSnapshot saved = snapshotWriter.write(id, tenantId, snapshot -> {
			// 재계산은 교체 후 스냅샷을 입력으로 해야 새 피토관 계수·노즐경이 반영된다.
			ScheduleSnapshot changed = snapshot.applyEquipmentChange(
				snapshot.team().withEquipments(equipments), snapshot.sheets());
			return changed.withSheets(reCalculator.reCalculate(changed, changed.sheets()));
		});

		return statusTransitioner.advanceAfterDocumentSaved(meta, saved);
	}

	/**
	 * 측정계획 문서의 의뢰기관(→사업장→측정시설) 스냅샷을 수정한다. 전달되지 않은 필드는 기존 값을 유지하며,
	 * 측정시설의 표준산소농도·굴뚝 형상 등은 계산 입력이므로 기존 시트를 새 값으로 재계산한다.
	 * 원장(tenant Client·Workplace·Stack)은 변경하지 않는다. 완료·취소된 계획은 변경할 수 없다.
	 */
	public ScheduleDetail changeClient(Long id, Long tenantId, ChangeClientSnapshotCommand command) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		meta.requireEditable();

		ClientSnapshot patch = new ClientSnapshot(
			null,                      // clientId는 원장 연결키이므로 변경 대상이 아니다.
			command.name(),
			command.bizNumber(),
			command.representative(),
			command.roadAddress(),
			command.detailAddress(),
			command.zipcode(),
			command.email(),
			command.tel(),
			command.workplace()
		);

		ScheduleSnapshot saved = snapshotWriter.write(id, tenantId, snapshot -> {
			// 재계산은 병합 후 스냅샷을 입력으로 해야 새 표준산소농도·굴뚝 형상이 반영된다.
			ScheduleSnapshot changed = snapshot.applyClientChange(patch, snapshot.sheets());
			return changed.withSheets(reCalculator.reCalculate(changed, changed.sheets()));
		});

		return statusTransitioner.advanceAfterDocumentSaved(meta, saved);
	}

	/**
	 * 성적서 기본정보 폼을 저장한다. 사용자에게는 한 폼이지만 값의 주인이 넷으로 갈려 있어 서버가 나눠 보낸다.
	 * <ul>
	 *   <li>시료접수일·분석완료일·성적서발행일 → <b>메타(MySQL)</b>. 성적서 진행 값의 진실은 메타다</li>
	 *   <li>채취 시각·배출시설관리자·시료채취입회자 → 채취 스냅샷. 그 회차 현장의 사실이다</li>
	 *   <li>시료분석검사자·기술책임자 → 고객사 스냅샷. 원장이 기본값을 갖지만 회차별로 다를 수 있다</li>
	 *   <li>측정자(사수·부사수) 표기 → 팀 스냅샷</li>
	 * </ul>
	 * 목적지가 넷이라고 API를 쪼개면 저장 한 번이 네 번의 왕복이 되고, 중간에 실패하면 화면 상태가 갈라진다.
	 * <p>
	 * 전달되지 않은(공백 포함) 값은 기존 값을 유지하는 <b>부분 갱신</b>이며, 원장은 어느 것도 건드리지 않는다.
	 * 시료접수일이 채워지면 분석 착수로 보고 상태를 전진시킨다({@link ScheduleProgress}).
	 * <p>
	 * 이 경로만 메타와 문서를 함께 쓴다. 저장 순서는 MySQL → Mongo다.
	 */
	public ScheduleDetail updateBasicInfo(Long id, Long tenantId, UpdateBasicInfoCommand command) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		meta.requireEditable();

		Schedule savedMeta = scheduleRepository.save(meta.applyReportProgress(
			command.receivedAt(), command.analyzedAt(), command.issuedAt()));

		ScheduleSnapshot changed = snapshotWriter.write(id, tenantId, snapshot -> snapshot
			.withSampling(samplingOf(snapshot).update(
				command.samplingStartedAt(), command.samplingEndedAt(),
				command.facilityManager(), command.samplingWitness()))
			.applyStaff(
				snapshot.tenant() == null ? null
					: snapshot.tenant().withStaff(command.analyst(), command.technicalManager()),
				snapshot.team() == null ? null
					: snapshot.team().withMembers(command.mentorName(), command.menteeName())));

		return statusTransitioner.advanceAfterDocumentSaved(savedMeta, changed);
	}

	/**
	 * 이번 측정계획에서 측정할 항목을 교체한다. 전달된 측정물질 목록으로 전체 교체하며,
	 * 이미 들어 있던 항목은 측정 시점 값(허용기준 등)을 그대로 유지하고 새로 추가된 항목만
	 * 측정시설 원장에서 조립한다. 측정시설에 등록되지 않은 물질은 거부한다.
	 * <p>
	 * 측정항목은 계산 입력이 아니므로 기존 시트는 재계산하지 않는다 — 빠진 항목의 측정값이
	 * 시트에 남아 있을 수 있으며, 정리는 측정 데이터 편집의 몫이다.
	 * 원장(측정시설·측정물질)은 변경하지 않는다. 완료·취소된 계획은 변경할 수 없다.
	 */
	public ScheduleDetail changeItems(Long id, Long tenantId, List<Long> pollutantIds) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		meta.requireEditable();

		ScheduleSnapshot changed = snapshotWriter.write(id, tenantId, snapshot ->
			snapshot.withItems(snapshotAssembler.resolveItems(meta, pollutantIds, snapshot.items())));

		return statusTransitioner.advanceAfterDocumentSaved(meta, changed);
	}

	/**
	 * 이번 계획의 측정항목 표기 순서를 바꾼다. 항목 집합은 그대로 두고 배열 순서만 재배치한다.
	 * <p>
	 * <b>이 순서가 곧 성적서의 항목 순서다.</b> 기록부 서식은 한 장에 실을 수 있는 항목 수가 정해져 있어
	 * (현대차 대기측정기록부는 4개) 템플릿이 인덱스로 칸을 지목하므로, 몇 번째 항목이 몇 번째 장
	 * 어느 칸에 들어갈지를 사용자가 여기서 정한다.
	 * <p>
	 * 요청 목록은 이 계획의 측정항목 전체여야 한다 — 부분 목록은 순서를 정의하지 못하므로 거절한다.
	 * 항목의 내용(허용기준 등)은 손대지 않고, 계산 입력도 아니므로 시트를 재계산하지 않는다.
	 * 원장(측정시설·측정물질)은 변경하지 않으며, 완료·취소된 계획은 변경할 수 없다.
	 */
	public ScheduleDetail reorderItems(Long id, Long tenantId, List<Long> orderedPollutantIds) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		meta.requireEditable();

		ScheduleSnapshot changed = snapshotWriter.write(id, tenantId, snapshot -> {
			// 집합 일치는 다시 읽은 문서를 기준으로 판정해야 한다 — 그 사이 항목이 바뀌었을 수 있다.
			scheduleValidator.requireExactItemOrder(snapshot.items(), orderedPollutantIds);
			return snapshot.withItemOrder(orderedPollutantIds);
		});

		return statusTransitioner.advanceAfterDocumentSaved(meta, changed);
	}

	/**
	 * 이번 계획에 담긴 측정항목 하나의 측정 조건(주기·허용기준·산소보정)을 바로잡는다.
	 * 계획을 세운 뒤 현장에서 배출허용기준이나 산소보정 적용 여부가 실제와 다름을 확인했을 때 쓰는 경로다.
	 * <p>
	 * 고치는 것은 <b>이 회차 문서</b>뿐이며 측정시설 원장(stack_pollutant)은 건드리지 않는다 —
	 * 원장까지 함께 고쳐야 하면 호출자가 원장 수정 API를 따로 호출한다. 반대로 원장 수정이
	 * 과거 회차로 흘러들지도 않는다.
	 * <p>
	 * 판정 근거와 분석 결과가 한 항목 안에 함께 있으므로 따로 맞출 것이 없다. 실험실 입력값은 손대지 않는다.
	 * <p>
	 * 측정 시트는 재계산하지 않는다 — 허용기준·산소보정 적용 여부는 초과 판정의 근거일 뿐
	 * 시트 계산식의 입력이 아니다(계산에 쓰이는 기준산소농도는 측정시설 스냅샷 소관이다).
	 * 완료된 회차의 이행 기록(measurement_record)은 재개방하면 해제되고 재완료 시 이 스냅샷으로
	 * 다시 남으므로, 완료·취소 상태를 여기서 열어 줄 이유가 없다.
	 */
	public ScheduleDetail updateItem(Long id, Long tenantId, Long pollutantId, UpdateScheduleItemCommand command) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		meta.requireEditable();

		ScheduleSnapshot changed = snapshotWriter.write(id, tenantId, snapshot ->
			snapshot.withItemReplaced(pollutantId, snapshot.requireItem(pollutantId)
				.applyCondition(command.cycle(), command.allowance(), command.oxygenApplicable())));

		return statusTransitioner.advanceAfterDocumentSaved(meta, changed);
	}

	/** 아직 채취 스냅샷이 없는 문서(구버전 백필 전)도 갱신 경로를 탈 수 있게 빈 스냅샷을 준다. */
	private static SamplingSnapshot samplingOf(ScheduleSnapshot snapshot) {
		return snapshot.samplingData() == null
			? SamplingSnapshot.create(null, null)
			: snapshot.samplingData();
	}
}
