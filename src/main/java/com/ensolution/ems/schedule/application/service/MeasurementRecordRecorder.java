package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.port.out.MeasurementRecordRepository;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.analysis.AnalysisRecord;
import com.ensolution.ems.schedule.domain.history.MeasurementRecord;
import com.ensolution.ems.schedule.domain.history.MeasurementResult;
import com.ensolution.ems.schedule.domain.snapshot.SamplingItemSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 측정항목별 회차 이력을 기록·해제한다. 이력 기록이 모든 상태 변경 경로에 얽히지 않도록
 * {@link ScheduleService}에서 분리해 둔다.
 * 호출하는 서비스의 트랜잭션에 참여하며, 실험분석정보(MongoDB) 조회도 그 안에서 일어난다.
 *
 * <p>기록의 근거는 <b>측정 시점 스냅샷</b>이지 원장이 아니다. 완료 시점에 원장을 다시 읽으면
 * 그 사이 바뀐 주기·허용기준이 과거 회차에 소급 적용된다.
 *
 * <p>결과값은 실험분석정보에서 옮겨 온다. 분석 입력은 계획이 완료되면 잠기므로
 * ({@link Schedule#requireEditable()}) 완료 시점의 분석 결과가 곧 확정값이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeasurementRecordRecorder {

	private final MeasurementRecordRepository measurementRecordRepository;
	private final AnalysisRecordIndexer analysisRecordIndexer;

	/**
	 * 완료된 측정계획의 측정항목을 결과값과 함께 이력으로 남긴다.
	 * <p>
	 * 기존 기록을 먼저 지우므로 여러 번 호출해도 결과가 같다 — 완료 → 재개방 → 재완료 경로에서
	 * 이행 해제가 누락되더라도 유니크 제약 위반(500)이 아니라 정상 재기록이 된다.
	 * <p>
	 * 분석 결과 조회를 지우기 전에 끝내 두는 이유는 쓰기를 시작하기 전에 입력을 모두 확보하기 위해서다.
	 * 조회가 실패하면 예외가 그대로 올라가 완료 처리 전체가 롤백된다 — 여기서 삼키면 결과값이 빈 행으로
	 * 멀쩡한 기존 기록을 덮어쓰게 된다.
	 */
	public void recordCompletion(Schedule completed, ScheduleSnapshot snapshot) {
		if (snapshot == null || snapshot.items() == null || snapshot.items().isEmpty()) return;
		if (completed.getSampledAt() == null) {
			log.warn("측정일이 없어 이력을 남기지 않습니다. scheduleId={}", completed.getId());
			return;
		}

		Map<Long, AnalysisRecord> analyses =
			analysisRecordIndexer.indexByPollutantId(completed.getId(), completed.getTenantId());

		measurementRecordRepository.deleteByScheduleId(completed.getId(), completed.getTenantId());

		List<MeasurementRecord> records = snapshot.items().stream()
			.filter(Objects::nonNull)
			.filter(this::identifiable)
			.map(item -> MeasurementRecord.ofCompletion(
				completed, item, MeasurementResult.ofCompletion(item, analyses.get(item.pollutantId()))))
			.toList();

		measurementRecordRepository.saveAll(records);
	}

	/** 재개방·취소·삭제로 완료가 풀릴 때 그 계획의 이행 기록을 되돌린다. */
	public void revoke(Long scheduleId, Long tenantId) {
		measurementRecordRepository.deleteByScheduleId(scheduleId, tenantId);
	}

	/**
	 * 어느 물질인지 특정할 수 없는 스냅샷은 건너뛴다. 측정물질 식별자 도입 이전 문서가 여기 걸리며,
	 * 이력을 남기지 못할 뿐 완료 처리 자체를 실패시키지는 않는다.
	 */
	private boolean identifiable(SamplingItemSnapshot item) {
		if (item.pollutantId() != null) return true;
		log.warn("측정물질 식별자가 없어 이력에서 제외합니다. pollutantName={}", item.nameKr());
		return false;
	}
}
