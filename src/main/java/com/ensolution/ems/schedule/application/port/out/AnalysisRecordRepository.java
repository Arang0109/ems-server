package com.ensolution.ems.schedule.application.port.out;

import com.ensolution.ems.schedule.domain.analysis.AnalysisRecord;

import java.util.List;

/** 실험분석정보(MongoDB) 아웃바운드 포트. 측정항목 하나당 문서 하나로 저장한다. */
public interface AnalysisRecordRepository {
	AnalysisRecord save(AnalysisRecord analysisRecord);
	AnalysisRecord findById(String id, Long tenantId);
	List<AnalysisRecord> findByScheduleId(Long scheduleId, Long tenantId);
	boolean existsByScheduleIdAndPollutantId(Long scheduleId, Long tenantId, Long pollutantId);
	void deleteById(String id, Long tenantId);

	/** 측정계획이 지워질 때 그 계획의 실험분석정보를 함께 지운다. */
	void deleteByScheduleId(Long scheduleId, Long tenantId);
}
