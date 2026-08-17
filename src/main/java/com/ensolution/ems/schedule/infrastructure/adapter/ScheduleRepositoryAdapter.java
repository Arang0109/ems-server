package com.ensolution.ems.schedule.infrastructure.adapter;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.port.out.ScheduleRepository;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.infrastructure.entity.ScheduleEntity;
import com.ensolution.ems.schedule.infrastructure.mapper.ScheduleEntityMapper;
import com.ensolution.ems.schedule.infrastructure.repository.ScheduleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional
public class ScheduleRepositoryAdapter implements ScheduleRepository {

	private final ScheduleJpaRepository scheduleJpaRepository;
	private final ScheduleEntityMapper mapper;

	@Override
	public Schedule save(Schedule schedule) {
		ScheduleEntity entity = mapper.toEntity(schedule);
		return mapper.toDomain(scheduleJpaRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public Schedule findById(Long id, Long tenantId) {
		return scheduleJpaRepository.findByScheduleIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Schedule> findAll(Long tenantId) {
		return mapper.toDomains(scheduleJpaRepository.findAllByTenantIdAndDeletedAtIsNull(tenantId));
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByStackIdAndTeamIdAndMeasureDate(Long tenantId, Long stackId, Long teamId, LocalDate sampledAt) {
		return scheduleJpaRepository
			.existsByTenantIdAndStackIdAndTeamIdAndSampledAtAndDeletedAtIsNull(tenantId, stackId, teamId, sampledAt);
	}

	@Override
	@Transactional(readOnly = true)
	public Schedule findByIdIncludingDeleted(Long id, Long tenantId) {
		return scheduleJpaRepository.findByScheduleIdAndTenantId(id, tenantId)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Schedule> findAllDeleted(Long tenantId) {
		return mapper.toDomains(scheduleJpaRepository.findAllByTenantIdAndDeletedAtIsNotNull(tenantId));
	}
}
