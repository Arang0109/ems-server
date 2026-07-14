package com.ensolution.ems.contract.infrastructure.repository;

import com.ensolution.ems.contract.infrastructure.entity.ContractTableViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractTableViewJpaRepository extends JpaRepository<ContractTableViewEntity, Long> {
	List<ContractTableViewEntity> findByTenantId(Long tenantId);
	List<ContractTableViewEntity> findByWorkplaceId(Long workplaceId);
}