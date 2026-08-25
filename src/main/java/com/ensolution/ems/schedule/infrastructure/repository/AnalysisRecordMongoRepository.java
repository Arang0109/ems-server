package com.ensolution.ems.schedule.infrastructure.repository;

import com.ensolution.ems.schedule.infrastructure.document.AnalysisRecordDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AnalysisRecordMongoRepository extends MongoRepository<AnalysisRecordDocument, String> {

	Optional<AnalysisRecordDocument> findByIdAndTenantId(String id, Long tenantId);

	List<AnalysisRecordDocument> findByScheduleIdAndTenantIdOrderByCreatedAtAsc(Long scheduleId, Long tenantId);

	boolean existsByScheduleIdAndTenantIdAndPollutantId(Long scheduleId, Long tenantId, Long pollutantId);

	void deleteByScheduleIdAndTenantId(Long scheduleId, Long tenantId);
}
