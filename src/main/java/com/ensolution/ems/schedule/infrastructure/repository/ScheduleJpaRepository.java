package com.ensolution.ems.schedule.infrastructure.repository;

import com.ensolution.ems.schedule.infrastructure.entity.ScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleJpaRepository extends JpaRepository<ScheduleEntity, Long> {

	Optional<ScheduleEntity> findByScheduleIdAndTenantId(Long scheduleId, Long tenantId);

	List<ScheduleEntity> findAllByTenantId(Long tenantId);

	boolean existsByStackIdAndTeamIdAndMeasureDate(Long stackId, Long teamId, LocalDateTime measureDate);

	@Modifying
	@Query("delete from ScheduleEntity s where s.scheduleId = :id and s.tenantId = :tenantId")
	int deleteByScheduleIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);
}
