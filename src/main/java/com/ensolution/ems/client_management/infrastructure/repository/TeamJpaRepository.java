package com.ensolution.ems.client_management.infrastructure.repository;

import com.ensolution.ems.client_management.infrastructure.entity.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamJpaRepository extends JpaRepository<TeamEntity, Long> {
	Optional<TeamEntity> findByTeamIdAndTenant_TenantId(Long teamId, Long tenantId);

	List<TeamEntity> findAllByTenant_TenantId(Long tenantId);

	/**
	 * 사수 또는 부사수로 배정된 팀을 조회한다.
	 * 1인 1팀이 원칙이나 기존 데이터에 중복 배정이 남아 있을 수 있으므로 단건이 아닌 목록으로 반환하고,
	 * 호출부가 결정적으로 동작하도록 teamId 오름차순으로 정렬한다.
	 */
	@Query("""
    select t from TeamEntity t
    where t.tenant.tenantId = :tenantId
      and (t.mentorUserId = :userId or t.menteeUserId = :userId)
    order by t.teamId asc
""")
	List<TeamEntity> findAllByMemberUserId(
		@Param("userId") Long userId,
		@Param("tenantId") Long tenantId
	);

	@Modifying
	@Query("""
    delete from TeamEntity t
    where t.teamId = :teamId
      and t.tenant.tenantId = :tenantId
""")
	int deleteByTeamIdAndTenantId(
		@Param("teamId") Long teamId,
		@Param("tenantId") Long tenantId
	);

	boolean existsByNameAndTenant_TenantId(String name, Long tenantId);
}
