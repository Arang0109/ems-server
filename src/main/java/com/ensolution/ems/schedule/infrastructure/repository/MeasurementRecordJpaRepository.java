package com.ensolution.ems.schedule.infrastructure.repository;

import com.ensolution.ems.schedule.infrastructure.entity.MeasurementRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeasurementRecordJpaRepository extends JpaRepository<MeasurementRecordEntity, Long> {

	List<MeasurementRecordEntity> findByTenantIdAndStackIdOrderBySampledAtDescRecordIdAsc(
		Long tenantId, Long stackId);

	List<MeasurementRecordEntity> findByTenantIdAndStackIdAndPeriodYearOrderBySampledAtDescRecordIdAsc(
		Long tenantId, Long stackId, Integer periodYear);

	List<MeasurementRecordEntity> findByTenantIdAndPeriodYear(Long tenantId, Integer periodYear);

	/**
	 * 이행 해제. 삭제할 행이 계획당 항목 수만큼이므로 엔티티를 로드하지 않고 벌크로 지운다.
	 * 호출부가 트랜잭션을 갖고 있으며, 이 시점 이후 같은 트랜잭션에서 이력을 다시 읽지 않는다.
	 */
	@Modifying
	@Query("""
		delete from MeasurementRecordEntity r
		where r.scheduleId = :scheduleId
		  and r.tenantId = :tenantId
	""")
	int deleteByScheduleIdAndTenantId(@Param("scheduleId") Long scheduleId, @Param("tenantId") Long tenantId);
}
