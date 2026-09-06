package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.service.support.SnapshotWriter;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.command.update.SaveAnalysisResultsCommand;
import com.ensolution.ems.schedule.application.command.update.SaveSamplingTimesCommand;
import com.ensolution.ems.schedule.application.port.out.ScheduleDocumentRepository;
import com.ensolution.ems.schedule.application.port.out.ScheduleRepository;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.snapshot.SamplingItemSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 실험분석정보 유스케이스. 측정계획(MySQL 메타)에 종속된 하위 리소스이므로 모든 경로에서
 * 먼저 계획을 확인하고, 완료·취소된 계획은 편집을 막는다({@link Schedule#requireEditable()}).
 *
 * <p>분석 결과는 측정계획 문서의 측정항목 안({@code items[].analysis})에 함께 저장된다.
 * 판정 근거(허용기준치·산소보정)와 결과값이 한 원소에 있어 둘이 갈라질 수 없고,
 * 항목과 결과를 잇는 색인도 필요 없다.
 *
 * <p><b>한 항목을 두 화면이 필드를 나눠 소유한다.</b> 실험·분석 탭은 실험실 입력값을
 * ({@link #saveAnalysisResults}), 성적서 탭은 채취시간을 ({@link #saveSamplingTimes}) 쓴다.
 * 경로가 갈라져 있고 각자 자기 필드만 건드리므로, 두 탭을 동시에 열어도 서로의 입력을
 * 덮어쓰지 않는다 — 같은 문서를 쓰게 된 뒤에도 그렇다. 동시 저장 규약은 {@link SnapshotWriter} 참고.
 *
 * <p>두 일괄 저장 모두 <b>측정물질(pollutantId)을 키로 upsert</b> 한다. 실제 불변식이
 * "한 계획의 한 측정항목 = 결과 하나"이므로 자연키로 쓰는 편이 맞다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AnalysisResultService {

	private final ScheduleRepository scheduleRepository;
	private final ScheduleDocumentRepository scheduleDocumentRepository;
	private final SnapshotWriter snapshotWriter;

	/**
	 * 실험·분석 탭의 항목별 분석 결과를 일괄 저장한다.
	 *
	 * <p>채취시간은 건드리지 않는다 — 성적서 탭이 소유하는 필드다. 반대로 <b>전달된 항목의 빈 값은
	 * 기존 값을 지운다</b>: 표 전체를 보내므로 빈 칸은 "미전달"이 아니라 "지웠다"는 뜻이다.
	 *
	 * <p>값이 모두 비어 있는데 기존 결과도 없으면 건너뛴다 — 한 번도 손대지 않은 행까지 빈 결과로
	 * 채우면 쓸모없는 값만 쌓인다.
	 */
	public List<SamplingItemSnapshot> saveAnalysisResults(
		Long scheduleId, Long tenantId, SaveAnalysisResultsCommand command
	) {
		List<SaveAnalysisResultsCommand.Entry> entries = command.items() == null ? List.of() : command.items();
		requireSavable(scheduleId, tenantId,
			entries.stream().map(SaveAnalysisResultsCommand.Entry::pollutantId).toList());

		ScheduleSnapshot saved = snapshotWriter.write(scheduleId, tenantId, snapshot -> {
			ScheduleSnapshot next = snapshot;
			for (SaveAnalysisResultsCommand.Entry entry : entries) {
				SamplingItemSnapshot item = next.requireItem(entry.pollutantId());
				if (item.analysis() == null && isBlank(entry)) continue;

				next = next.withItemReplaced(entry.pollutantId(), item.withAnalysis(
					item.analysisOrEmpty().applyAnalysisResult(
						entry.analysisValue(), entry.unit(),
						entry.analysisMethod(), entry.analysisEquipment())));
			}
			return next;
		});
		return saved.items();
	}

	/**
	 * 성적서 탭의 항목별 채취시간을 일괄 저장한다.
	 *
	 * <p>실험실 입력값은 건드리지 않는다 — 실험·분석 탭이 소유하는 필드다. 반대로 <b>전달된 항목의
	 * 빈 시각은 기존 값을 지운다</b>: 성적서 탭이 표 전체를 보내므로 빈 칸은 "지웠다"는 뜻이다.
	 * 요청에 아예 없는 항목은 손대지 않는다.
	 */
	public List<SamplingItemSnapshot> saveSamplingTimes(
		Long scheduleId, Long tenantId, SaveSamplingTimesCommand command
	) {
		List<SaveSamplingTimesCommand.Entry> entries = command.items() == null ? List.of() : command.items();
		requireSavable(scheduleId, tenantId,
			entries.stream().map(SaveSamplingTimesCommand.Entry::pollutantId).toList());

		ScheduleSnapshot saved = snapshotWriter.write(scheduleId, tenantId, snapshot -> {
			ScheduleSnapshot next = snapshot;
			for (SaveSamplingTimesCommand.Entry entry : entries) {
				SamplingItemSnapshot item = next.requireItem(entry.pollutantId());
				if (item.analysis() == null
					&& entry.samplingStartedAt() == null && entry.samplingEndedAt() == null) continue;

				next = next.withItemReplaced(entry.pollutantId(), item.withAnalysis(
					item.analysisOrEmpty().applySamplingTime(
						entry.samplingStartedAt(), entry.samplingEndedAt())));
			}
			return next;
		});
		return saved.items();
	}

	/** 측정항목과 그에 딸린 분석 결과를 함께 읽는다. */
	@Transactional(readOnly = true)
	public List<SamplingItemSnapshot> getAnalyses(Long scheduleId, Long tenantId) {
		scheduleRepository.findById(scheduleId, tenantId);
		return scheduleDocumentRepository.findByScheduleId(scheduleId, tenantId).items();
	}

	/**
	 * 두 일괄 저장이 공유하는 전제 — 계획이 편집 가능하고, 요청 안에 같은 항목이 두 번 담기지 않아야 한다.
	 * 스냅샷이 근거인 검증({@code requireItem})은 문서를 다시 읽는 쪽에서 해야 하므로
	 * {@link SnapshotWriter}의 변경 함수 안에 남긴다.
	 */
	private void requireSavable(Long scheduleId, Long tenantId, List<Long> pollutantIds) {
		Schedule meta = scheduleRepository.findById(scheduleId, tenantId);
		meta.requireEditable();
		requireUniquePollutants(pollutantIds);
	}

	/**
	 * 요청 안에 같은 측정항목이 두 번 담기지 않았는지 확인한다.
	 * 포트 조회가 필요 없는 순수 검증이라 Validator로 뽑지 않는다.
	 */
	private static void requireUniquePollutants(List<Long> pollutantIds) {
		Set<Long> seen = new HashSet<>();
		for (Long pollutantId : pollutantIds) {
			if (pollutantId != null && !seen.add(pollutantId)) {
				throw new CustomException(ErrorCode.SCHEDULE_ANALYSIS_DUPLICATE_ITEM);
			}
		}
	}

	/** 네 칸이 모두 비어 있으면 아직 손대지 않은 행이다. */
	private static boolean isBlank(SaveAnalysisResultsCommand.Entry entry) {
		return entry.analysisValue() == null
			&& isBlank(entry.unit())
			&& isBlank(entry.analysisMethod())
			&& isBlank(entry.analysisEquipment());
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
