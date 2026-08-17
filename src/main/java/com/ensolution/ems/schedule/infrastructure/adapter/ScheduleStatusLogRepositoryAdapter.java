package com.ensolution.ems.schedule.infrastructure.adapter;

import com.ensolution.ems.schedule.application.port.out.ScheduleStatusLogRepository;
import com.ensolution.ems.schedule.domain.ScheduleStatusLog;
import com.ensolution.ems.schedule.infrastructure.mapper.ScheduleStatusLogEntityMapper;
import com.ensolution.ems.schedule.infrastructure.repository.ScheduleStatusLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional
public class ScheduleStatusLogRepositoryAdapter implements ScheduleStatusLogRepository {

	private final ScheduleStatusLogJpaRepository scheduleStatusLogJpaRepository;
	private final ScheduleStatusLogEntityMapper mapper;

	@Override
	public ScheduleStatusLog save(ScheduleStatusLog log) {
		return mapper.toDomain(scheduleStatusLogJpaRepository.save(mapper.toEntity(log)));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ScheduleStatusLog> findByScheduleId(Long scheduleId, Long tenantId) {
		return mapper.toDomainList(
			scheduleStatusLogJpaRepository.findByScheduleIdAndTenantIdOrderByChangedAtAsc(scheduleId, tenantId));
	}
}
