package com.ensolution.ems.schedule.infrastructure.repository;

import com.ensolution.ems.schedule.infrastructure.document.ScheduleDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleDocumentMongoRepository extends MongoRepository<ScheduleDocument, String> {

	Optional<ScheduleDocument> findByScheduleIdAndTenantId(Long scheduleId, Long tenantId);

	List<ScheduleDocument> findByTenantId(Long tenantId);

	void deleteByScheduleIdAndTenantId(Long scheduleId, Long tenantId);

	/** 저장 직전 생성 시각만 읽어오기 위한 프로젝션. 문서 본문을 통째로 실어 오지 않는다. */
	Optional<CreatedAtView> findCreatedAtById(String id);

	interface CreatedAtView {
		LocalDateTime getCreatedAt();
	}
}
