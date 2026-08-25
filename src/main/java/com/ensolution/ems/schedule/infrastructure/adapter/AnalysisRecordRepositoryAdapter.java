package com.ensolution.ems.schedule.infrastructure.adapter;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.port.out.AnalysisRecordRepository;
import com.ensolution.ems.schedule.domain.analysis.AnalysisRecord;
import com.ensolution.ems.schedule.infrastructure.document.AnalysisRecordDocument;
import com.ensolution.ems.schedule.infrastructure.mapper.AnalysisRecordDocumentMapper;
import com.ensolution.ems.schedule.infrastructure.repository.AnalysisRecordMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AnalysisRecordRepositoryAdapter implements AnalysisRecordRepository {

	private final AnalysisRecordMongoRepository analysisRecordMongoRepository;
	private final AnalysisRecordDocumentMapper mapper;

	@Override
	public AnalysisRecord save(AnalysisRecord analysisRecord) {
		AnalysisRecordDocument saved = analysisRecordMongoRepository.save(mapper.toDocument(analysisRecord));
		return mapper.toDomain(saved);
	}

	@Override
	public AnalysisRecord findById(String id, Long tenantId) {
		return mapper.toDomain(requireDocument(id, tenantId));
	}

	@Override
	public List<AnalysisRecord> findByScheduleId(Long scheduleId, Long tenantId) {
		return mapper.toDomains(
			analysisRecordMongoRepository.findByScheduleIdAndTenantIdOrderByCreatedAtAsc(scheduleId, tenantId));
	}

	@Override
	public boolean existsByScheduleIdAndPollutantId(Long scheduleId, Long tenantId, Long pollutantId) {
		return analysisRecordMongoRepository
			.existsByScheduleIdAndTenantIdAndPollutantId(scheduleId, tenantId, pollutantId);
	}

	@Override
	public void deleteById(String id, Long tenantId) {
		analysisRecordMongoRepository.delete(requireDocument(id, tenantId));
	}

	@Override
	public void deleteByScheduleId(Long scheduleId, Long tenantId) {
		analysisRecordMongoRepository.deleteByScheduleIdAndTenantId(scheduleId, tenantId);
	}

	private AnalysisRecordDocument requireDocument(String id, Long tenantId) {
		return analysisRecordMongoRepository.findByIdAndTenantId(id, tenantId)
			.orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_ANALYSIS_NOT_FOUND));
	}
}
