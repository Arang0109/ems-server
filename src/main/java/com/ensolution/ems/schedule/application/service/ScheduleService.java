package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.command.create.CreateScheduleCommand;
import com.ensolution.ems.schedule.application.command.detail.PreviousSheetCandidate;
import com.ensolution.ems.schedule.application.command.detail.PreviousSheetDetail;
import com.ensolution.ems.schedule.application.command.detail.ScheduleDetail;
import com.ensolution.ems.schedule.application.command.event.EditorRef;
import com.ensolution.ems.schedule.application.command.event.SheetsSavedEvent;
import com.ensolution.ems.schedule.application.command.list_item.ScheduleListItem;
import com.ensolution.ems.schedule.application.command.update.ChangeScheduleEquipmentsCommand;
import com.ensolution.ems.schedule.application.command.update.ChangeClientSnapshotCommand;
import com.ensolution.ems.schedule.application.command.update.UpdateBasicInfoCommand;
import com.ensolution.ems.schedule.application.command.update.UpdateScheduleCommand;
import com.ensolution.ems.schedule.application.command.update.UpdateScheduleItemCommand;
import com.ensolution.ems.schedule.application.mapper.ScheduleListItemMapper;
import com.ensolution.ems.schedule.application.port.in.MonthlyMeasurementCount;
import com.ensolution.ems.schedule.application.port.in.ScheduleStatisticsUseCase;
import com.ensolution.ems.schedule.application.port.out.AnalysisRecordRepository;
import com.ensolution.ems.schedule.application.port.out.ScheduleDocumentRepository;
import com.ensolution.ems.schedule.application.port.out.ScheduleEventBroadcaster;
import com.ensolution.ems.schedule.application.port.out.ScheduleRepository;
import com.ensolution.ems.schedule.application.validator.ScheduleValidator;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.ScheduleProgress;
import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.domain.sheet.MeasurementCategory;
import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import com.ensolution.ems.schedule.domain.sheet.SheetMerge;
import com.ensolution.ems.schedule.domain.sheet.SheetRef;
import com.ensolution.ems.schedule.domain.snapshot.*;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 측정계획 유스케이스. 메타(MySQL)를 진실의 원천으로 두고, 세부 스냅샷(MongoDB) 저장/삭제를
 * 각 트랜잭션의 마지막 부수효과로 배치해 정합성을 확보한다(2PC 불가).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService implements ScheduleStatisticsUseCase {

	private final ScheduleRepository scheduleRepository;
	private final ScheduleDocumentRepository scheduleDocumentRepository;
	private final ScheduleSnapshotAssembler snapshotAssembler;
	private final SnapshotSheetRecalculator recalculator;
	private final MeasurementRecordRecorder measurementRecordRecorder;
	private final PreviousSheetFinder previousSheetFinder;
	private final ScheduleValidator scheduleValidator;
	private final ScheduleEventBroadcaster eventBroadcaster;
	private final AnalysisRecordRepository analysisRecordRepository;
	private final ScheduleListItemMapper scheduleListItemMapper;

	// 문서 단위 낙관적 락에 걸렸을 때 다시 읽어 병합을 시도하는 횟수(물리적 동시 저장 대비).
	private static final int MAX_SHEET_SAVE_ATTEMPTS = 3;

	public ScheduleDetail createSchedule(CreateScheduleCommand command) {
		scheduleValidator.requireUniqueSchedule(
			command.tenantId(), command.stackId(), command.teamId(), command.sampledAt());

		Schedule saved = scheduleRepository.save(Schedule.register(
			command.tenantId(),
			command.stackId(),
			command.teamId(),
			command.measurementField(),
			command.sampledAt(),
			command.schedulePurpose(),
			command.referenceNumber()
		));

		ScheduleSnapshot snapshot = snapshotAssembler.assemble(saved, command.pollutantIds());
		ScheduleSnapshot savedSnapshot = scheduleDocumentRepository.save(snapshot);
		return new ScheduleDetail(saved, savedSnapshot);
	}

	public ScheduleDetail updateMeta(Long id, Long tenantId, UpdateScheduleCommand command) {
		Schedule schedule = scheduleRepository.findById(id, tenantId);
		schedule.requireEditable();

		Schedule saved = scheduleRepository.save(schedule.update(
			command.measurementField(),
			command.sampledAt(),
			command.schedulePurpose(),
			command.referenceNumber()
		));

		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);
		// 측정·성적서 발행 단계에서 채워지는 값(담당자·접수/분석/발행일자·채취 시각)이
		// 메타 수정으로 유실되지 않도록 기존 기본 정보에 병합한다.
		BasicInfo basicInfo = basicInfoOf(snapshot, saved).applyMeta(
			saved.getReferenceNumber(),
			saved.getSampledAt(),
			saved.getMeasurementField(),
			saved.getSchedulePurpose());
		ScheduleSnapshot savedSnapshot = scheduleDocumentRepository.save(
			snapshot.applyMetaUpdate(basicInfo, command.tenant()));
		return new ScheduleDetail(saved, savedSnapshot);
	}

	/**
	 * 측정계획 문서의 기본 정보를 수정한다. 담당자(배출시설관리자·시료채취입회자·시료분석검사자·기술책임자)·
	 * 시료접수/분석완료/성적서발행일자·채취 시작/종료 시각과 측정자 표기를 부분 갱신하며,
	 * 전달되지 않은 필드는 기존 값을 유지한다.
	 * 계산 입력이 아닌 값만 다루므로 측정 시트는 재계산하지 않는다. 원장(tenant)은 변경하지 않으며,
	 * 완료·취소된 계획은 수정할 수 없다.
	 * 갱신 결과에 따른 자동 전이(접수일자 입력 → 분석값 입력 중 등)는 {@link ScheduleProgress}가 판정한다.
	 */
	public ScheduleDetail updateBasicInfo(Long id, Long tenantId, UpdateBasicInfoCommand command) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		meta.requireEditable();

		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);
		BasicInfo basicInfo = basicInfoOf(snapshot, meta).update(
			command.facilityManager(),
			command.samplingWitness(),
			command.analyst(),
			command.technicalManager(),
			command.receivedAt(),
			command.analyzedAt(),
			command.issuedAt(),
			command.samplingStartedAt(),
			command.samplingEndedAt()
		);
		TeamSnapshot team = snapshot.team() == null
			? null
			: snapshot.team().withMembers(command.mentorName(), command.menteeName());

		ScheduleSnapshot updated = snapshot.applyBasicInfo(basicInfo, team);
		return saveAdvanced(meta, ScheduleProgress.advance(meta, updated), updated);
	}

	/** 문서의 기본 정보를 반환한다. 기본 정보 없이 저장된 과거 문서는 메타에서 새로 조립한다. */
	private BasicInfo basicInfoOf(ScheduleSnapshot snapshot, Schedule meta) {
		if (snapshot.basicInfo() != null) return snapshot.basicInfo();
		return BasicInfo.create(
			meta.getReferenceNumber(),
			meta.getSampledAt(),
			meta.getMeasurementField(),
			meta.getSchedulePurpose(),
			null,
			null);
	}

	/** 분석을 마친 측정계획을 완료로 확정한다. 이후 편집이 잠기며 측정 건수 통계에 집계된다. */
	public ScheduleDetail complete(Long id, Long tenantId) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		return applyStatusChange(meta, meta.complete());
	}

	/**
	 * 업무가 무산된 측정계획을 취소한다. 삭제와 달리 계획은 목록에 남는다.
	 * 완료·취소된 계획은 다시 취소할 수 없다.
	 */
	public ScheduleDetail cancel(Long id, Long tenantId) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		return applyStatusChange(meta, meta.cancel());
	}

	/**
	 * 종단 상태(완료·취소)를 풀어 다시 작업할 수 있게 한다.
	 * <p>
	 * 돌아갈 단계는 스냅샷에서 재도출한다 — 실측값이 있으면 측정 중, 시료접수일까지 있으면 분석값 입력 중이다.
	 * 취소는 세 단계 어디에서든 걸 수 있어 되돌릴 지점이 하나로 정해지지 않는데, 진행 단계가 원래
	 * 스냅샷에서 파생되는 값이므로 같은 답이 나온다.
	 * 재개방하면 상태가 완료가 아니게 되므로 측정 건수 통계에서도 자동으로 빠진다.
	 */
	public ScheduleDetail reopen(Long id, Long tenantId) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);

		Schedule cleared = meta.reopen();
		return applyStatusChange(meta, ScheduleProgress.advance(cleared, snapshot), snapshot);
	}

	/** 사용자가 확정한 상태 전이를 저장한다. 저장 순서는 MySQL → Mongo다. */
	private ScheduleDetail applyStatusChange(Schedule meta, Schedule changed) {
		return applyStatusChange(meta, changed,
			scheduleDocumentRepository.findByScheduleId(meta.getId(), meta.getTenantId()));
	}

	private ScheduleDetail applyStatusChange(
		Schedule meta, Schedule changed, ScheduleSnapshot snapshot
	) {
		Schedule saved = scheduleRepository.save(changed);
		syncMeasurementRecords(meta, saved, snapshot);

		ScheduleSnapshot savedSnapshot = scheduleDocumentRepository.save(snapshot.syncStatus(saved.getStatus()));
		return new ScheduleDetail(saved, savedSnapshot);
	}

	/**
	 * 완료 여부의 변화를 측정항목 이력에 반영한다. 완료로 확정되면 항목별 이행을 남기고,
	 * 완료가 풀리면(재개방) 그 계획이 만든 이행을 되돌린다.
	 * <p>
	 * 사용자가 확정하는 전이가 모두 {@link #applyStatusChange}를 지나므로 훅이 여기 한 곳이면 충분하다.
	 * 자동 전이({@link #saveAdvanced})는 분석값 입력 중까지만 전진시키므로 성적서 작성 완료 경계를 넘지 않는다.
	 */
	private void syncMeasurementRecords(Schedule before, Schedule after, ScheduleSnapshot snapshot) {
		if (after.getStatus() == ScheduleStatus.REPORT_COMPLETED) {
			measurementRecordRecorder.recordCompletion(after, snapshot);
		} else if (before.getStatus() == ScheduleStatus.REPORT_COMPLETED) {
			measurementRecordRecorder.revoke(after.getId(), after.getTenantId());
		}
	}

	/**
	 * 자동 전이 결과를 반영해 저장한다. 상태가 실제로 바뀐 경우에만 메타(MySQL)를 저장하고 스냅샷 상태를
	 * 동기화하므로, 전이가 없는 일반 저장은 문서 단독 쓰기로 남는다.
	 * 저장 순서는 {@link #applyStatusChange}와 동일하게 MySQL → Mongo다.
	 */
	private ScheduleDetail saveAdvanced(Schedule meta, Schedule advanced, ScheduleSnapshot snapshot) {
		if (advanced.getStatus() == meta.getStatus()) {
			return new ScheduleDetail(meta, scheduleDocumentRepository.save(snapshot));
		}

		Schedule saved = scheduleRepository.save(advanced);
		ScheduleSnapshot savedSnapshot = scheduleDocumentRepository.save(snapshot.syncStatus(saved.getStatus()));
		return new ScheduleDetail(saved, savedSnapshot);
	}

	/**
	 * 잘못 등록된 측정계획을 지운다. 되돌릴 수 없으므로 실측 데이터가 없는 '측정 예정'과 '취소'에서만
	 * 가능하며, 진행 중인 계획은 취소로 처리해야 한다.
	 * 메타(MySQL)와 세부 문서(MongoDB)를 함께 지우며, 삭제 순서는 저장과 반대로 Mongo → MySQL이다.
	 */
	public void deleteSchedule(Long id, Long tenantId) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		meta.requireDeletable();

		// 삭제 가능한 상태(측정 예정·취소)에는 완료 이력이 있을 수 없다. 그래도 지우는 이유는,
		// 이력 백필처럼 상태와 기록이 어긋날 수 있는 경로가 있어 지운 계획의 이행이 현황판에 남지 않게 하기 위해서다.
		measurementRecordRecorder.revoke(id, tenantId);
		analysisRecordRepository.deleteByScheduleId(id, tenantId);
		scheduleDocumentRepository.deleteByScheduleId(id, tenantId);
		scheduleRepository.deleteById(id, tenantId);
	}

	/**
	 * 측정 시트를 저장한다. 저장 시 계산 파이프라인을 실행해 계산 결과가 반영된 시트를 함께 저장한다.
	 * 완료·취소된 계획은 편집할 수 없다. 피토관 계수는 스냅샷의 팀 장비에서, 표준산소농도는 각 시트 입력에서 취한다.
	 * 측정점에 실측값이 들어오면 측정 착수로 보고 상태를 전진시킨다(사용자의 별도 조작이 필요 없다).
	 * 시트 틀만 저장하는 경로는 착수로 보지 않으며, 판정은 {@link ScheduleProgress}가 담당한다.
	 * <p>
	 * 여러 사용자가 같은 측정계획을 동시에 편집할 수 있으므로 요청 시트를 서버 보관본에 병합한다
	 * ({@link SheetMerge}). 요청이 읽어간 뒤 같은 시트가 먼저 저장됐으면 409로 거부하고,
	 * 서로 다른 시트를 건드린 경우에는 양쪽 입력이 모두 남는다.
	 * <p>
	 * 저장이 끝나면 같은 계획을 열어둔 다른 사용자에게 알린다({@link SheetsSavedEvent}). 한 기록지를
	 * 섹션별로 나눠 입력하는 것이 실제 업무 방식이라, 상대 입력이 즉시 반영되지 않으면 화면의 계산값이
	 * 서로 어긋난 채로 남는다 — 계산 입력이 섹션을 가로질러 엮여 있기 때문이다.
	 */
	public ScheduleDetail saveSheets(Long id, Long tenantId, EditorRef editor,
	                                 List<MeasurementSheet> sheets, List<SheetRef> deletedSheets) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		meta.requireEditable();

		ScheduleSnapshot saved = mergeAndSaveSheets(id, tenantId, sheets, deletedSheets);
		ScheduleDetail detail = advanceStatus(meta, saved);

		publishAfterCommit(new SheetsSavedEvent(
			id, tenantId, editor,
			touchedCategories(sheets, deletedSheets),
			detail.meta().getStatus()));

		return detail;
	}

	/** 이번 저장이 건드린 시트 카테고리. 수정분과 삭제분을 합친다 — 둘 다 상대 화면에서 갱신돼야 한다. */
	private static List<MeasurementCategory> touchedCategories(List<MeasurementSheet> sheets,
	                                                           List<SheetRef> deletedSheets) {
		return Stream.concat(
				nullSafe(sheets).stream().map(MeasurementSheet::getCategory),
				nullSafe(deletedSheets).stream().map(SheetRef::category))
			.filter(Objects::nonNull)
			.distinct()
			.toList();
	}

	private static <T> List<T> nullSafe(List<T> values) {
		return values == null ? List.of() : values;
	}

	/**
	 * 커밋이 끝난 뒤에 알림을 보낸다. 이 서비스는 클래스 레벨 {@code @Transactional}이라 그냥 호출하면
	 * 뒤이어 롤백될 저장까지 "저장됐다"고 알리게 된다. 알림 전송 실패가 저장을 되돌리는 일도 없어야 하므로
	 * 커밋 이후가 맞다.
	 * <p>
	 * 트랜잭션 밖에서 호출된 경우(테스트 등 동기화 비활성)에는 즉시 발행한다.
	 */
	private void publishAfterCommit(SheetsSavedEvent event) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			eventBroadcaster.publishSheetsSaved(event);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				eventBroadcaster.publishSheetsSaved(event);
			}
		});
	}

	/**
	 * 문서를 읽어 요청 시트를 병합하고 저장한다. 저장 직전에 다른 요청이 같은 문서를 먼저 쓰면
	 * 문서 단위 낙관적 락이 걸리는데(MongoDB {@code @Version}), 이는 "같은 시트를 고쳤다"는 뜻이 아니라
	 * 두 저장이 물리적으로 겹쳤다는 뜻이므로 다시 읽어 병합을 재시도한다. 논리적 충돌(같은 시트를
	 * 먼저 저장함)은 병합 단계의 시트 version 비교가 이미 걸러내 예외로 빠져나간다.
	 */
	private ScheduleSnapshot mergeAndSaveSheets(Long id, Long tenantId,
	                                            List<MeasurementSheet> sheets, List<SheetRef> deletedSheets) {
		for (int attempt = 1; attempt <= MAX_SHEET_SAVE_ATTEMPTS; attempt++) {
			ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);
			List<MeasurementSheet> merged = SheetMerge.merge(snapshot.sheets(), sheets, deletedSheets);
			ScheduleSnapshot recalculated = snapshot.withSheets(recalculator.recalculate(snapshot, merged));

			try {
				return scheduleDocumentRepository.save(recalculated);
			} catch (OptimisticLockingFailureException e) {
				if (attempt == MAX_SHEET_SAVE_ATTEMPTS) {
					throw new CustomException(ErrorCode.SCHEDULE_SHEET_VERSION_CONFLICT,
						ErrorCode.SCHEDULE_SHEET_VERSION_CONFLICT.getMessage(), e);
				}
			}
		}
		throw new CustomException(ErrorCode.SCHEDULE_SHEET_VERSION_CONFLICT);
	}

	/**
	 * 저장된 스냅샷을 근거로 메타 상태를 전진시킨다. 상태가 그대로면 메타는 건드리지 않는다.
	 * {@link #saveAdvanced}와 달리 문서는 이미 저장된 뒤이므로, 상태가 바뀔 때만 문서를 한 번 더 쓴다.
	 */
	private ScheduleDetail advanceStatus(Schedule meta, ScheduleSnapshot saved) {
		Schedule advanced = ScheduleProgress.advance(meta, saved);
		if (advanced.getStatus() == meta.getStatus()) {
			return new ScheduleDetail(meta, saved);
		}

		Schedule savedMeta = scheduleRepository.save(advanced);
		return new ScheduleDetail(savedMeta, scheduleDocumentRepository.save(saved.syncStatus(savedMeta.getStatus())));
	}

	/**
	 * 측정계획의 측정장비를 교체한다. 전달되지 않은 슬롯은 기존 장비를 유지하며, 팀 스냅샷의 장비 id와
	 * 장비 목록을 함께 갱신하고 기존 시트를 새 장비 spec으로 재계산한다.
	 * 장비는 메타(MySQL)가 아닌 문서(MongoDB)에만 존재하므로 상태 전이가 없는 한 문서 단독 쓰기이며,
	 * 원장(tenant·equipment)은 변경하지 않는다. 완료·취소된 계획은 변경할 수 없다.
	 */
	public ScheduleDetail changeEquipments(Long id, Long tenantId, ChangeScheduleEquipmentsCommand command) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		meta.requireEditable();

		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);
		TeamSnapshot newTeam = snapshot.team().withEquipmentIds(
			command.particleSamplerId(),
			command.gasSamplerId(),
			command.pitotTubeId(),
			command.nozzleId()
		);
		List<EquipmentSnapshot> newEquipments = snapshotAssembler.resolveEquipments(newTeam, tenantId);

		// 재계산은 교체 후 스냅샷을 입력으로 해야 새 피토관 계수·노즐경이 반영된다.
		ScheduleSnapshot changed = snapshot.applyEquipmentChange(newTeam, newEquipments, snapshot.sheets());
		ScheduleSnapshot recalculated = changed.withSheets(recalculator.recalculate(changed, changed.sheets()));

		return saveAdvanced(meta, ScheduleProgress.advance(meta, recalculated), recalculated);
	}

	/**
	 * 측정계획 문서의 의뢰기관(→사업장→측정시설) 스냅샷을 수정한다. 전달되지 않은 필드는 기존 값을 유지하며,
	 * 측정시설의 표준산소농도·굴뚝 형상 등은 계산 입력이므로 기존 시트를 새 값으로 재계산한다.
	 * 원장(tenant Client·Workplace·Stack)은 변경하지 않는다. 완료·취소된 계획은 변경할 수 없다.
	 */
	public ScheduleDetail changeClient(Long id, Long tenantId, ChangeClientSnapshotCommand command) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		meta.requireEditable();

		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);
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

		// 재계산은 병합 후 스냅샷을 입력으로 해야 새 표준산소농도·굴뚝 형상이 반영된다.
		ScheduleSnapshot changed = snapshot.applyClientChange(patch, snapshot.sheets());
		ScheduleSnapshot recalculated = changed.withSheets(recalculator.recalculate(changed, changed.sheets()));

		return saveAdvanced(meta, ScheduleProgress.advance(meta, recalculated), recalculated);
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

		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);
		List<SamplingItemSnapshot> newItems =
			snapshotAssembler.resolveItems(meta, pollutantIds, snapshot.items());

		ScheduleSnapshot changed = snapshot.withItems(newItems);
		return saveAdvanced(meta, ScheduleProgress.advance(meta, changed), changed);
	}

	/**
	 * 이번 계획의 측정항목 표기 순서를 바꾼다. 항목 집합은 그대로 두고 배열 순서만 재배치한다.
	 * <p>
	 * <b>이 순서가 곧 성적서의 항목 순서다.</b> 기록부 서식은 한 장에 실을 수 있는 항목 수가 정해져 있어
	 * (현대차 대기측정기록부는 4개) 템플릿이 {@code ${items[0].name}} 처럼 인덱스로 칸을 지목하므로,
	 * 몇 번째 항목이 몇 번째 장 어느 칸에 들어갈지를 사용자가 여기서 정한다.
	 * <p>
	 * 요청 목록은 이 계획의 측정항목 전체여야 한다 — 부분 목록은 순서를 정의하지 못하므로 거절한다.
	 * 항목의 내용(허용기준 등)은 손대지 않고, 계산 입력도 아니므로 시트를 재계산하지 않는다.
	 * 원장(측정시설·측정물질)은 변경하지 않으며, 완료·취소된 계획은 변경할 수 없다.
	 */
	public ScheduleDetail reorderItems(Long id, Long tenantId, List<Long> orderedPollutantIds) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		meta.requireEditable();

		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);
		scheduleValidator.requireExactItemOrder(snapshot.items(), orderedPollutantIds);

		ScheduleSnapshot changed = snapshot.withItemOrder(orderedPollutantIds);
		return saveAdvanced(meta, ScheduleProgress.advance(meta, changed), changed);
	}

	/**
	 * 이번 계획에 담긴 측정항목 하나의 측정 조건(주기·허용기준·산소보정)을 바로잡는다.
	 * 계획을 세운 뒤 현장에서 배출허용기준이나 산소보정 적용 여부가 실제와 다름을 확인했을 때 쓰는 경로다.
	 * <p>
	 * 고치는 것은 <b>이 회차 문서</b>뿐이며 측정시설 원장(stack_pollutant)은 건드리지 않는다 —
	 * 원장까지 함께 고쳐야 하면 호출자가 원장 수정 API를 따로 호출한다. 반대로 원장 수정이
	 * 과거 회차로 흘러들지도 않는다.
	 * <p>
	 * 같은 항목의 실험분석정보가 이미 있으면 판정 근거를 함께 맞춘다. 한 회차 안에서 허용기준이
	 * 두 값으로 갈라지면 어느 쪽이 성적서의 기준인지 알 수 없기 때문이다. 실험실 입력값은 손대지 않는다.
	 * <p>
	 * 측정 시트는 재계산하지 않는다 — 허용기준·산소보정 적용 여부는 초과 판정의 근거일 뿐
	 * 시트 계산식의 입력이 아니다(계산에 쓰이는 기준산소농도는 측정시설 스냅샷 소관이다).
	 * 완료된 회차의 이행 기록(measurement_record)은 재개방하면 해제되고 재완료 시 이 스냅샷으로
	 * 다시 남으므로, 완료·취소 상태를 여기서 열어 줄 이유가 없다.
	 */
	public ScheduleDetail updateItem(Long id, Long tenantId, Long pollutantId, UpdateScheduleItemCommand command) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		meta.requireEditable();

		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);
		SamplingItemSnapshot corrected = snapshot.requireItem(pollutantId)
			.applyCondition(command.cycle(), command.allowance(), command.oxygenApplicable());

		syncAnalysisJudgementBasis(id, tenantId, pollutantId, corrected);

		ScheduleSnapshot changed = snapshot.withItemReplaced(pollutantId, corrected);
		return saveAdvanced(meta, ScheduleProgress.advance(meta, changed), changed);
	}

	/**
	 * 정정된 측정항목의 판정 근거를 같은 항목의 실험분석정보에도 반영한다.
	 * 분석 결과가 아직 없으면 할 일이 없다 — 나중에 등록될 때 정정된 스냅샷에서 복사된다.
	 */
	private void syncAnalysisJudgementBasis(
		Long scheduleId, Long tenantId, Long pollutantId, SamplingItemSnapshot corrected
	) {
		analysisRecordRepository.findByScheduleId(scheduleId, tenantId).stream()
			.filter(analysis -> pollutantId.equals(analysis.getPollutantId()))
			.map(analysis -> analysis.syncJudgementBasis(corrected))
			.forEach(analysisRecordRepository::save);
	}

	@Transactional(readOnly = true)
	public ScheduleDetail getSchedule(Long id, Long tenantId) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);
		return new ScheduleDetail(meta, snapshot);
	}

	/**
	 * 새로 추가한 기록지를 채울 이전 회차 시트를 조회한다. <b>저장하지는 않는다</b> —
	 * 사용자가 값을 확인하고 고친 뒤 기존 시트 저장 경로로 보내야, 동시 편집 보호(시트 버전)를
	 * 우회하는 쓰기 경로가 생기지 않고 잘못 눌렀을 때도 되돌릴 일이 없다.
	 * <p>
	 * 불러올 기록이 없으면 null이다. 첫 회차이거나 그 기록지를 처음 쓰는 경우이며 오류가 아니다.
	 */
	@Transactional(readOnly = true)
	public PreviousSheetDetail getPreviousSheet(
		Long id, Long tenantId, MeasurementCategory category, Long sourceScheduleId
	) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		return previousSheetFinder.find(meta, category, sourceScheduleId);
	}

	/**
	 * 불러올 수 있는 이전 회차 목록을 최신순으로 조회한다. 가장 최근 회차가 늘 좋은 출발점은 아니라
	 * (이상 조업이었던 회차 등) 사용자가 어느 회차에서 가져올지 고를 수 있어야 한다.
	 * <p>
	 * 시트 본문은 담기지 않는다 - 고른 뒤 {@link #getPreviousSheet}로 그 회차만 받아 간다.
	 */
	@Transactional(readOnly = true)
	public List<PreviousSheetCandidate> getPreviousSheetCandidates(
		Long id, Long tenantId, MeasurementCategory category
	) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		return previousSheetFinder.findCandidates(meta, category);
	}

	@Transactional(readOnly = true)
	public List<ScheduleListItem> getScheduleList(Long tenantId) {
		return toListItems(
			scheduleRepository.findAll(tenantId).stream()
				.filter(schedule -> schedule.getStatus() != ScheduleStatus.CANCELED)
				.toList(),
			tenantId);
	}

	/** 취소된 측정계획 목록을 반환한다. */
	@Transactional(readOnly = true)
	public List<ScheduleListItem> getCanceledScheduleList(Long tenantId) {
		List<Schedule> canceled = scheduleRepository.findAll(tenantId).stream()
			.filter(schedule -> schedule.getStatus() == ScheduleStatus.CANCELED)
			.toList();

		return toListItems(canceled, tenantId);
	}

	/** 메타 목록에 세부 문서를 조인해 목록 아이템으로 조립한다. */
	private List<ScheduleListItem> toListItems(List<Schedule> metas, Long tenantId) {
		Map<Long, ScheduleSnapshot> snapshotByScheduleId = scheduleDocumentRepository.findAll(tenantId).stream()
			.collect(Collectors.toMap(ScheduleSnapshot::scheduleId, Function.identity()));

		return metas.stream()
			.map(meta -> scheduleListItemMapper.toListItem(meta, snapshotByScheduleId.get(meta.getId())))
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public long countCompleted(Long tenantId) {
		return scheduleRepository.findAll(tenantId).stream()
			.filter(ScheduleService::isCompleted)
			.count();
	}

	@Override
	@Transactional(readOnly = true)
	public long countCompletedInMonth(Long tenantId, YearMonth yearMonth) {
		return scheduleRepository.findAll(tenantId).stream()
			.filter(ScheduleService::isCompleted)
			.filter(schedule -> yearMonth.equals(YearMonth.from(schedule.getSampledAt())))
			.count();
	}

	@Override
	@Transactional(readOnly = true)
	public List<MonthlyMeasurementCount> monthlyCompletedCounts(Long tenantId, int year) {
		Map<Integer, Long> countByMonth = scheduleRepository.findAll(tenantId).stream()
			.filter(ScheduleService::isCompleted)
			.filter(schedule -> schedule.getSampledAt().getYear() == year)
			.collect(Collectors.groupingBy(
				schedule -> schedule.getSampledAt().getMonthValue(),
				Collectors.counting()));

		return IntStream.rangeClosed(1, 12)
			.mapToObj(month -> new MonthlyMeasurementCount(month, countByMonth.getOrDefault(month, 0L)))
			.toList();
	}

	/** 완료 상태이면서 집계 기준일(measureDate)을 가진 측정계획인지 여부. */
	private static boolean isCompleted(Schedule schedule) {
		return schedule.getStatus() == ScheduleStatus.REPORT_COMPLETED && schedule.getSampledAt() != null;
	}
}
