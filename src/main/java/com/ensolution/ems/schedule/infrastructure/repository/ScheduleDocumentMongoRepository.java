package com.ensolution.ems.schedule.infrastructure.repository;

import com.ensolution.ems.schedule.infrastructure.document.ScheduleDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ScheduleDocumentMongoRepository extends MongoRepository<ScheduleDocument, String> {

	Optional<ScheduleDocument> findByScheduleIdAndTenantId(Long scheduleId, Long tenantId);

	List<ScheduleDocument> findByTenantId(Long tenantId);

	void deleteByScheduleIdAndTenantId(Long scheduleId, Long tenantId);
}
