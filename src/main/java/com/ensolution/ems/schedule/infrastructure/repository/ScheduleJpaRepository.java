package com.ensolution.ems.schedule.infrastructure.repository;

import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.infrastructure.entity.ScheduleEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** 측정계획 메타 JPA 저장소. 삭제는 물리 삭제이므로 조회에 별도 필터가 없다. */
@Repository
public interface ScheduleJpaRepository extends JpaRepository<ScheduleEntity, Long> {

	Optional<ScheduleEntity> findByScheduleIdAndTenantId(Long scheduleId, Long tenantId);

	List<ScheduleEntity> findAllByTenantIdOrderBySampledAtDescReferenceNumber(Long tenantId);

	boolean existsByTenantIdAndStackIdAndTeamIdAndSampledAt(
		Long tenantId, Long stackId, Long teamId, LocalDate sampledAt);

	/**
	 * 이전 기록지 조회용. 같은 측정시설의 완료 계획을 채취일 내림차순으로 반환한다.
	 * 같은 날 두 건이 있으면 나중에 등록된 것을 앞에 둔다.
	 */
	List<ScheduleEntity> findByTenantIdAndStackIdAndStatusAndSampledAtLessThanOrderBySampledAtDescScheduleIdDesc(
		Long tenantId, Long stackId, ScheduleStatus status, LocalDate sampledAt, Limit limit);
}
