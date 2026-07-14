package com.ensolution.ems.schedule.infrastructure.adapter;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.port.out.ScheduleDocumentRepository;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import com.ensolution.ems.schedule.infrastructure.document.ScheduleDocument;
import com.ensolution.ems.schedule.infrastructure.mapper.ScheduleDocumentMapper;
import com.ensolution.ems.schedule.infrastructure.repository.ScheduleDocumentMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ScheduleDocumentRepositoryAdapter implements ScheduleDocumentRepository {

	private final ScheduleDocumentMongoRepository scheduleDocumentMongoRepository;
	private final ScheduleDocumentMapper mapper;

	@Override
	public ScheduleSnapshot save(ScheduleSnapshot snapshot) {
		ScheduleDocument saved = scheduleDocumentMongoRepository.save(mapper.toDocument(snapshot));
		return mapper.toDomain(saved);
	}

	@Override
	public ScheduleSnapshot findByScheduleId(Long scheduleId, Long tenantId) {
		return scheduleDocumentMongoRepository.findByScheduleIdAndTenantId(scheduleId, tenantId)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_DOCUMENT_NOT_FOUND));
	}

	@Override
	public List<ScheduleSnapshot> findAll(Long tenantId) {
		return mapper.toDomains(scheduleDocumentMongoRepository.findByTenantId(tenantId));
	}

	@Override
	public void deleteByScheduleId(Long scheduleId, Long tenantId) {
		scheduleDocumentMongoRepository.deleteByScheduleIdAndTenantId(scheduleId, tenantId);
	}
}
