package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.command.detail.PreviousSheetCandidate;
import com.ensolution.ems.schedule.application.command.detail.PreviousSheetDetail;
import com.ensolution.ems.schedule.application.command.detail.ScheduleDetail;
import com.ensolution.ems.schedule.application.event.EditorRef;
import com.ensolution.ems.schedule.application.event.SheetsSavedEvent;
import com.ensolution.ems.schedule.application.port.out.ScheduleEventBroadcaster;
import com.ensolution.ems.schedule.application.port.out.ScheduleRepository;
import com.ensolution.ems.schedule.application.service.support.PreviousSheetFinder;
import com.ensolution.ems.schedule.application.service.support.ScheduleStatusTransitioner;
import com.ensolution.ems.schedule.application.service.support.SnapshotSheetRecalculator;
import com.ensolution.ems.schedule.application.service.support.SnapshotWriter;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.ScheduleProgress;
import com.ensolution.ems.schedule.domain.sampling.MeasurementCategory;
import com.ensolution.ems.schedule.domain.sampling.SamplingSheet;
import com.ensolution.ems.schedule.domain.sampling.SheetMerge;
import com.ensolution.ems.schedule.domain.sampling.SheetRef;
import com.ensolution.ems.schedule.domain.snapshot.SamplingSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 측정 시트(기록지) 유스케이스. 저장과 이전 회차 불러오기를 담당한다.
 *
 * <p>다른 수정 경로와 갈라 둔 이유는 이 경로에만 붙는 규약이 셋이기 때문이다 —
 * <b>시트 단위 버전 충돌 판정</b>({@link SheetMerge}), <b>계산 파이프라인 재실행</b>,
 * <b>저장 완료 SSE 알림</b>. 그리고 상태가 {@code ANALYZING}에 이르면 기록지가 잠긴다
 * ({@link Schedule#requireSheetEditable()}) — 다른 경로에는 없는 두 번째 관문이다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleSheetService {

	private final ScheduleRepository scheduleRepository;
	private final SnapshotWriter snapshotWriter;
	private final SnapshotSheetRecalculator recalculator;
	private final PreviousSheetFinder previousSheetFinder;
	private final ScheduleEventBroadcaster eventBroadcaster;
	private final ScheduleStatusTransitioner statusTransitioner;

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
	 * 채취 시각과 현장 담당자도 같은 저장에서 함께 반영한다 — 같은 채취 스냅샷 노드에 살고
	 * 현장 채취 탭이 함께 소유하므로, 나눠 보내면 저장 한 번이 여러 왕복이 되고 중간에 실패하면
	 * 화면 상태가 갈라진다. 대신 기록지와 잠금 시점을 공유한다 — {@code ANALYZING}부터는
	 * 이 넷도 함께 잠긴다({@code requireSheetEditable}).
	 * <p>
	 * 저장이 끝나면 같은 계획을 열어둔 다른 사용자에게 알린다({@link SheetsSavedEvent}). 한 기록지를
	 * 섹션별로 나눠 입력하는 것이 실제 업무 방식이라, 상대 입력이 즉시 반영되지 않으면 화면의 계산값이
	 * 서로 어긋난 채로 남는다 — 계산 입력이 섹션을 가로질러 엮여 있기 때문이다.
	 */
	public ScheduleDetail saveSheets(Long id, Long tenantId, EditorRef editor,
	                                 LocalTime samplingStartedAt, LocalTime samplingEndedAt,
	                                 String facilityManager, String samplingWitness,
	                                 List<SamplingSheet> sheets, List<SheetRef> deletedSheets) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		meta.requireEditable();
		meta.requireSheetEditable();

		ScheduleSnapshot saved = mergeAndSaveSheets(
			id, tenantId, samplingStartedAt, samplingEndedAt, facilityManager, samplingWitness,
			sheets, deletedSheets);
		ScheduleDetail detail = statusTransitioner.advanceAfterDocumentSaved(meta, saved);

		publishAfterCommit(new SheetsSavedEvent(
			id, tenantId, editor,
			touchedCategories(sheets, deletedSheets),
			detail.meta().getStatus()));

		return detail;
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

	/**
	 * 문서를 읽어 요청 시트를 병합하고 저장한다. 재계산은 병합 결과를 입력으로 해야 하므로 안에서 한다.
	 * 물리적으로 겹친 저장을 다시 읽어 재시도하는 것은 {@link SnapshotWriter}가 맡는다 — 논리적 충돌
	 * (같은 시트를 먼저 저장함)은 병합 단계의 시트 version 비교가 이미 걸러내 예외로 빠져나간다.
	 */
	private ScheduleSnapshot mergeAndSaveSheets(
		Long id, Long tenantId,
		LocalTime samplingStartedAt, LocalTime samplingEndedAt,
		String facilityManager, String samplingWitness,
		List<SamplingSheet> sheets, List<SheetRef> deletedSheets
	) {
		return snapshotWriter.write(id, tenantId, snapshot -> {
			List<SamplingSheet> merged = SheetMerge.merge(snapshot.sheets(), sheets, deletedSheets);
			ScheduleSnapshot withSheets = snapshot.withSheets(recalculator.recalculate(snapshot, merged));
			return withSheets.withSampling(samplingOf(withSheets).update(
				samplingStartedAt, samplingEndedAt, facilityManager, samplingWitness));
		});
	}

	/** 아직 채취 스냅샷이 없는 문서(구버전 백필 전)도 갱신 경로를 탈 수 있게 빈 스냅샷을 준다. */
	private static SamplingSnapshot samplingOf(ScheduleSnapshot snapshot) {
		return snapshot.samplingData() == null
			? SamplingSnapshot.create(null, null)
			: snapshot.samplingData();
	}

	/** 이번 저장이 건드린 시트 카테고리. 수정분과 삭제분을 합친다 — 둘 다 상대 화면에서 갱신돼야 한다. */
	private static List<MeasurementCategory> touchedCategories(List<SamplingSheet> sheets,
	                                                           List<SheetRef> deletedSheets) {
		return Stream.concat(
				nullSafe(sheets).stream().map(SamplingSheet::getCategory),
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
}
