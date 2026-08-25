package com.ensolution.ems.schedule.application;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.port.out.AnalysisRecordRepository;
import com.ensolution.ems.schedule.domain.analysis.AnalysisRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 인메모리 {@link AnalysisRecordRepository}. */
public class FakeAnalysisRecordRepository implements AnalysisRecordRepository {

	private final List<AnalysisRecord> records = new ArrayList<>();
	private long nextId = 1L;

	public List<AnalysisRecord> all() {
		return List.copyOf(records);
	}

	@Override
	public AnalysisRecord save(AnalysisRecord analysisRecord) {
		AnalysisRecord saved = analysisRecord.getId() == null
			? analysisRecord.toBuilder().id("analysis-" + nextId++).build()
			: analysisRecord;
		records.removeIf(record -> Objects.equals(record.getId(), saved.getId()));
		records.add(saved);
		return saved;
	}

	@Override
	public AnalysisRecord findById(String id, Long tenantId) {
		return records.stream()
			.filter(record -> Objects.equals(record.getId(), id))
			.filter(record -> Objects.equals(record.getTenantId(), tenantId))
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_ANALYSIS_NOT_FOUND));
	}

	@Override
	public List<AnalysisRecord> findByScheduleId(Long scheduleId, Long tenantId) {
		return records.stream()
			.filter(record -> Objects.equals(record.getScheduleId(), scheduleId))
			.filter(record -> Objects.equals(record.getTenantId(), tenantId))
			.toList();
	}

	@Override
	public boolean existsByScheduleIdAndPollutantId(Long scheduleId, Long tenantId, Long pollutantId) {
		return records.stream()
			.filter(record -> Objects.equals(record.getScheduleId(), scheduleId))
			.filter(record -> Objects.equals(record.getTenantId(), tenantId))
			.anyMatch(record -> Objects.equals(record.getPollutantId(), pollutantId));
	}

	@Override
	public void deleteById(String id, Long tenantId) {
		records.removeIf(record ->
			Objects.equals(record.getId(), id) && Objects.equals(record.getTenantId(), tenantId));
	}

	@Override
	public void deleteByScheduleId(Long scheduleId, Long tenantId) {
		records.removeIf(record ->
			Objects.equals(record.getScheduleId(), scheduleId) && Objects.equals(record.getTenantId(), tenantId));
	}
}
