package com.ensolution.ems.schedule.infrastructure.repository;

import com.ensolution.ems.schedule.infrastructure.entity.ScheduleStatusLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleStatusLogJpaRepository extends JpaRepository<ScheduleStatusLogEntity, Long> {

	List<ScheduleStatusLogEntity> findByScheduleIdAndTenantIdOrderByChangedAtAsc(Long scheduleId, Long tenantId);
}
