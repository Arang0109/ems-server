package com.ensolution.ems.schedule.application.service.support;

import com.ensolution.ems.schedule.application.command.detail.ScheduleDetail;
import com.ensolution.ems.schedule.application.port.out.ScheduleDocumentRepository;
import com.ensolution.ems.schedule.application.port.out.ScheduleRepository;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.ScheduleProgress;
import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 측정계획 상태 전이의 저장 시점을 한곳에 모은다.
 *
 * <p>전이 경로가 둘인데 <b>문서(MongoDB)를 언제 쓰는지가 다르다.</b> 흩어 두면 이름이 비슷해
 * 서로 바꿔 쓰기 쉬우므로, 여기 모아 메서드명이 저장 시점을 말하게 한다.
 *
 * <table>
 *   <caption>전이 경로별 문서 저장 시점</caption>
 *   <tr><th>메서드</th><th>문서 저장</th><th>쓰는 곳</th></tr>
 *   <tr><td>{@link #confirmTransition}</td><td><b>안 씀</b> — 상태는 메타에만 있어 되비출 것이 없다</td>
 *       <td>사용자 확정 전이 (완료·취소·재개방)</td></tr>
 *   <tr><td>{@link #advanceAfterDocumentSaved}</td><td><b>이미 저장됨</b> — {@link SnapshotWriter}가 썼다</td>
 *       <td>문서 락을 지나는 경로</td></tr>
 * </table>
 *
 * <p>저장 순서는 어느 경로든 MySQL → Mongo다(2PC 불가라 메타를 진실의 원천으로 둔다).
 * 완료 여부 변화를 이행 이력에 반영하는 훅은 {@code confirmTransition} 안에만 있다 —
 * 자동 전이는 분석값 입력 중까지만 전진시켜 성적서 작성 완료 경계를 넘지 않기 때문이다
 * ({@link ScheduleProgress}).
 *
 * <p>협력자 규약대로 {@code @Transactional}을 붙이지 않는다. 호출하는 Service의 트랜잭션에 참여한다.
 */
@Component
@RequiredArgsConstructor
public class ScheduleStatusTransitioner {

	private final ScheduleRepository scheduleRepository;
	private final ScheduleDocumentRepository scheduleDocumentRepository;
	private final MeasurementRecordRecorder measurementRecordRecorder;

	/** 사용자가 확정한 상태 전이를 저장한다. 문서는 판단 근거로 읽기만 한다. */
	public ScheduleDetail confirmTransition(Schedule meta, Schedule changed) {
		return confirmTransition(meta, changed,
			scheduleDocumentRepository.findByScheduleId(meta.getId(), meta.getTenantId()));
	}

	/** 이미 읽어 둔 문서가 있을 때 쓰는 오버로드. 재개방은 되돌아갈 단계를 문서에서 재도출한다. */
	public ScheduleDetail confirmTransition(Schedule meta, Schedule changed, ScheduleSnapshot snapshot) {
		Schedule saved = scheduleRepository.save(changed);
		syncMeasurementRecords(meta, saved, snapshot);

		// 상태는 메타에만 있으므로 문서를 다시 쓸 이유가 없다.
		return new ScheduleDetail(saved, snapshot);
	}

	/**
	 * 이미 저장된 스냅샷을 근거로 메타 상태를 전진시킨다. 상태가 그대로면 메타도 건드리지 않는다.
	 * 문서는 {@link SnapshotWriter}가 쓴 뒤이고 상태는 메타에만 있으므로 여기서 문서를 다시 쓰지 않는다.
	 */
	public ScheduleDetail advanceAfterDocumentSaved(Schedule meta, ScheduleSnapshot saved) {
		Schedule advanced = ScheduleProgress.advance(meta, saved);
		if (advanced.getStatus() == meta.getStatus()) {
			return new ScheduleDetail(meta, saved);
		}
		return new ScheduleDetail(scheduleRepository.save(advanced), saved);
	}

	/**
	 * 완료 여부의 변화를 측정항목 이력에 반영한다. 완료로 확정되면 항목별 이행을 남기고,
	 * 완료가 풀리면(재개방) 그 계획이 만든 이행을 되돌린다.
	 */
	private void syncMeasurementRecords(Schedule before, Schedule after, ScheduleSnapshot snapshot) {
		if (after.getStatus() == ScheduleStatus.REPORT_COMPLETED) {
			measurementRecordRecorder.recordCompletion(after, snapshot);
		} else if (before.getStatus() == ScheduleStatus.REPORT_COMPLETED) {
			measurementRecordRecorder.revoke(after.getId(), after.getTenantId());
		}
	}
}
