package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.command.create.CreateAnalysisRecordCommand;
import com.ensolution.ems.schedule.application.command.update.SaveAnalysisResultsCommand;
import com.ensolution.ems.schedule.application.command.update.SaveSamplingTimesCommand;
import com.ensolution.ems.schedule.application.command.update.UpdateAnalysisRecordCommand;
import com.ensolution.ems.schedule.application.port.out.AnalysisRecordRepository;
import com.ensolution.ems.schedule.application.port.out.ScheduleDocumentRepository;
import com.ensolution.ems.schedule.application.port.out.ScheduleRepository;
import com.ensolution.ems.schedule.application.validator.AnalysisRecordValidator;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.analysis.AnalysisRecord;
import com.ensolution.ems.schedule.domain.snapshot.SamplingItemSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 실험분석정보 유스케이스. 측정계획(MySQL 메타)에 종속된 하위 리소스이므로 모든 경로에서
 * 먼저 계획을 확인하고, 완료·취소된 계획은 편집을 막는다({@link Schedule#requireEditable()}).
 *
 * <p>측정 시트와 달리 계획 문서에 얹지 않고 별도 컬렉션에 항목당 한 건으로 저장하므로,
 * 실험실 입력이 현장 측정값 저장과 서로를 기다리거나 충돌하지 않는다.
 *
 * <p><b>한 기록을 두 화면이 필드를 나눠 소유한다.</b> 실험·분석 탭은 실험실 입력값을
 * ({@link #saveAnalysisResults}), 성적서 탭은 채취시간을 ({@link #saveSamplingTimes}) 쓴다.
 * 경로가 갈라져 있어 두 탭을 동시에 열어도 서로의 입력을 덮어쓰지 않는다.
 *
 * <p>두 일괄 저장 모두 <b>측정물질(pollutantId)을 키로 upsert</b> 한다. 문서 id로 신규·기존을
 * 판별하게 두면 한 탭이 문서를 만든 사실을 다른 탭이 모른 채 등록을 시도해 409로 막힌다 —
 * 실제 불변식이 "한 계획의 한 측정항목 = 문서 하나"이므로 자연키로 쓰는 편이 맞다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AnalysisRecordService {

	private final ScheduleRepository scheduleRepository;
	private final ScheduleDocumentRepository scheduleDocumentRepository;
	private final AnalysisRecordRepository analysisRecordRepository;
	private final AnalysisRecordValidator analysisRecordValidator;
	private final AnalysisRecordIndexer analysisRecordIndexer;

	/**
	 * 측정항목 하나의 분석 결과를 등록한다. 허용기준치·산소보정 적용 여부·측정물질명은
	 * 이번 계획의 측정항목 스냅샷(측정 시점 원장 사본)에서 복사한다 — 원장을 다시 읽으면
	 * 그 사이 개정된 허용기준이 과거 회차에 소급 적용된다.
	 * 이번 계획의 측정항목이 아닌 물질은 거부한다.
	 */
	public AnalysisRecord createAnalysis(Long scheduleId, Long tenantId, CreateAnalysisRecordCommand command) {
		Schedule meta = scheduleRepository.findById(scheduleId, tenantId);
		meta.requireEditable();
		analysisRecordValidator.requireUniquePollutant(scheduleId, tenantId, command.pollutantId());

		SamplingItemSnapshot item = scheduleDocumentRepository
			.findByScheduleId(scheduleId, tenantId)
			.requireItem(command.pollutantId());

		return analysisRecordRepository.save(AnalysisRecord.register(
			tenantId,
			scheduleId,
			item,
			command.analysisValue(),
			command.unit(),
			command.analysisMethod(),
			command.analysisEquipment()
		));
	}

	/** 실험실 입력값을 수정한다. 전달되지 않은 필드는 기존 값을 유지한다. */
	public AnalysisRecord updateAnalysis(Long scheduleId, Long tenantId, String analysisId,
	                                     UpdateAnalysisRecordCommand command) {
		AnalysisRecord analysisRecord = requireOwnedRecord(scheduleId, tenantId, analysisId);

		return analysisRecordRepository.save(analysisRecord.update(
			command.analysisValue(),
			command.unit(),
			command.analysisMethod(),
			command.analysisEquipment()
		));
	}

	/**
	 * 실험·분석 탭의 항목별 분석 결과를 일괄 저장한다. 기록이 없는 항목은 새로 만들고, 있으면 값만 갈아끼운다.
	 *
	 * <p>채취시간은 건드리지 않는다({@link AnalysisRecord#applyAnalysisResult}) — 성적서 탭이
	 * 소유하는 필드다. 반대로 <b>전달된 항목의 빈 값은 기존 값을 지운다</b>: 표 전체를 보내므로
	 * 빈 칸은 "미전달"이 아니라 "지웠다"는 뜻이다.
	 *
	 * <p>값이 모두 비어 있는데 기존 기록도 없으면 건너뛴다 — 한 번도 손대지 않은 행까지 빈 문서로
	 * 만들면 쓸모없는 기록만 쌓인다.
	 */
	public List<AnalysisRecord> saveAnalysisResults(Long scheduleId, Long tenantId, SaveAnalysisResultsCommand command) {
		List<SaveAnalysisResultsCommand.Entry> entries = command.items() == null ? List.of() : command.items();
		UpsertContext context = prepareUpsert(scheduleId, tenantId,
			entries.stream().map(SaveAnalysisResultsCommand.Entry::pollutantId).toList());

		List<AnalysisRecord> saved = new ArrayList<>(entries.size());
		for (SaveAnalysisResultsCommand.Entry entry : entries) {
			SamplingItemSnapshot item = context.snapshot().requireItem(entry.pollutantId());
			AnalysisRecord current = context.existing().get(entry.pollutantId());

			if (current == null && isBlank(entry)) continue;

			AnalysisRecord next = current == null
				? AnalysisRecord.register(tenantId, scheduleId, item,
					entry.analysisValue(), entry.unit(), entry.analysisMethod(), entry.analysisEquipment())
				: current.applyAnalysisResult(
					entry.analysisValue(), entry.unit(), entry.analysisMethod(), entry.analysisEquipment());

			saved.add(analysisRecordRepository.save(next));
		}
		return saved;
	}

	/** 네 칸이 모두 비어 있으면 아직 손대지 않은 행이다. */
	private boolean isBlank(SaveAnalysisResultsCommand.Entry entry) {
		return entry.analysisValue() == null
			&& isBlank(entry.unit())
			&& isBlank(entry.analysisMethod())
			&& isBlank(entry.analysisEquipment());
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	/**
	 * 성적서 탭의 항목별 채취시간을 일괄 저장한다. 기록이 없는 항목은 새로 만들고, 있으면 시각만 갈아끼운다.
	 *
	 * <p>실험실 입력값은 건드리지 않는다({@link AnalysisRecord#applySamplingTime}) — 실험·분석 탭이
	 * 소유하는 필드다. 반대로 <b>전달된 항목의 빈 시각은 기존 값을 지운다</b>: 성적서 탭이 표 전체를
	 * 보내므로 빈 칸은 "미전달"이 아니라 "지웠다"는 뜻이다. 요청에 아예 없는 항목은 손대지 않는다.
	 *
	 * <p>시각이 둘 다 비어 있는데 기존 기록도 없으면 건너뛴다 — 성적서 탭은 측정항목 전체를 행으로
	 * 그리므로, 한 번도 손대지 않은 행까지 빈 문서로 만들면 쓸모없는 기록만 쌓인다.
	 */
	public List<AnalysisRecord> saveSamplingTimes(Long scheduleId, Long tenantId, SaveSamplingTimesCommand command) {
		List<SaveSamplingTimesCommand.Entry> entries = command.items() == null ? List.of() : command.items();
		UpsertContext context = prepareUpsert(scheduleId, tenantId,
			entries.stream().map(SaveSamplingTimesCommand.Entry::pollutantId).toList());

		List<AnalysisRecord> saved = new ArrayList<>(entries.size());
		for (SaveSamplingTimesCommand.Entry entry : entries) {
			SamplingItemSnapshot item = context.snapshot().requireItem(entry.pollutantId());
			AnalysisRecord current = context.existing().get(entry.pollutantId());

			if (current == null && entry.samplingStartedAt() == null && entry.samplingEndedAt() == null) continue;

			AnalysisRecord next = current == null
				? AnalysisRecord.ofSamplingTime(
					tenantId, scheduleId, item, entry.samplingStartedAt(), entry.samplingEndedAt())
				: current.applySamplingTime(entry.samplingStartedAt(), entry.samplingEndedAt());

			saved.add(analysisRecordRepository.save(next));
		}
		return saved;
	}

	/**
	 * 두 일괄 저장이 공유하는 전제 — 계획이 편집 가능하고, 요청 안에 같은 항목이 두 번 담기지 않았으며,
	 * 모든 항목이 이번 계획의 측정항목이어야 한다(항목 확인은 호출부의 {@code requireItem}이 마저 한다).
	 * <p>
	 * <b>쓰기 전에 검증과 조회를 모두 끝낸다.</b> 중간에 거절되면 아무것도 저장되지 않아야 표를 다시
	 * 고쳐 보낼 수 있다.
	 */
	private UpsertContext prepareUpsert(Long scheduleId, Long tenantId, List<Long> pollutantIds) {
		Schedule meta = scheduleRepository.findById(scheduleId, tenantId);
		meta.requireEditable();

		analysisRecordValidator.requireUniquePollutants(pollutantIds);

		return new UpsertContext(
			scheduleDocumentRepository.findByScheduleId(scheduleId, tenantId),
			analysisRecordIndexer.indexByPollutantId(scheduleId, tenantId));
	}

	/** 일괄 upsert의 입력 — 측정항목 스냅샷과 측정물질로 색인한 기존 기록. */
	private record UpsertContext(ScheduleSnapshot snapshot, Map<Long, AnalysisRecord> existing) {}

	/** 잘못 입력된 분석 결과를 지운다. 같은 항목을 다시 등록할 수 있게 된다. */
	public void deleteAnalysis(Long scheduleId, Long tenantId, String analysisId) {
		requireOwnedRecord(scheduleId, tenantId, analysisId);
		analysisRecordRepository.deleteById(analysisId, tenantId);
	}

	@Transactional(readOnly = true)
	public List<AnalysisRecord> getAnalyses(Long scheduleId, Long tenantId) {
		scheduleRepository.findById(scheduleId, tenantId);
		return analysisRecordRepository.findByScheduleId(scheduleId, tenantId);
	}

	@Transactional(readOnly = true)
	public AnalysisRecord getAnalysis(Long scheduleId, Long tenantId, String analysisId) {
		scheduleRepository.findById(scheduleId, tenantId);
		AnalysisRecord analysisRecord = analysisRecordRepository.findById(analysisId, tenantId);
		analysisRecord.requireBelongsTo(scheduleId);
		return analysisRecord;
	}

	/** 편집 경로의 공통 전제 — 계획이 편집 가능하고, 그 계획에 속한 기록이어야 한다. */
	private AnalysisRecord requireOwnedRecord(Long scheduleId, Long tenantId, String analysisId) {
		Schedule meta = scheduleRepository.findById(scheduleId, tenantId);
		meta.requireEditable();

		AnalysisRecord analysisRecord = analysisRecordRepository.findById(analysisId, tenantId);
		analysisRecord.requireBelongsTo(scheduleId);
		return analysisRecord;
	}
}
