package com.ensolution.ems.tenant.infrastructure.repository;

import com.ensolution.ems.tenant.infrastructure.entity.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientJpaRepository extends JpaRepository<ClientEntity, Long> {
	List<ClientEntity> findAllByTenantTenantId(Long tenantId);
	Optional<ClientEntity> findByClientIdAndTenant_TenantId(Long clientId, Long tenantId);
	
	@Modifying
	@Query("""
    delete from ClientEntity c
    where c.clientId = :clientId
      and c.tenant.tenantId = :tenantId
""")
	int deleteByClientIdAndTenantId(
		@Param("clientId") Long clientId,
		@Param("tenantId") Long tenantId
	);
	
	boolean existsByName(String name);
}
