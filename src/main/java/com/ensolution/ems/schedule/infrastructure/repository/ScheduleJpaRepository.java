package com.ensolution.ems.schedule.infrastructure.repository;

import com.ensolution.ems.schedule.infrastructure.entity.ScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 측정계획 메타 JPA 저장소.
 * 삭제는 soft delete이므로 일반 조회는 모두 {@code DeletedAtIsNull} 조건을 포함한다.
 * 필터가 없는 메서드는 관리자 복구 경로 전용이다.
 */
@Repository
public interface ScheduleJpaRepository extends JpaRepository<ScheduleEntity, Long> {

	Optional<ScheduleEntity> findByScheduleIdAndTenantIdAndDeletedAtIsNull(Long scheduleId, Long tenantId);

	List<ScheduleEntity> findAllByTenantIdAndDeletedAtIsNull(Long tenantId);

	boolean existsByTenantIdAndStackIdAndTeamIdAndSampledAtAndDeletedAtIsNull(
		Long tenantId, Long stackId, Long teamId, LocalDate sampledAt);

	// ===== 관리자 복구 경로 (삭제된 건 포함) =====

	Optional<ScheduleEntity> findByScheduleIdAndTenantId(Long scheduleId, Long tenantId);

	List<ScheduleEntity> findAllByTenantIdAndDeletedAtIsNotNull(Long tenantId);
}
