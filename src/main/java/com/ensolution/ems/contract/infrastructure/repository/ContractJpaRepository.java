package com.ensolution.ems.contract.infrastructure.repository;

import com.ensolution.ems.contract.infrastructure.entity.ContractEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractJpaRepository extends JpaRepository<ContractEntity, Long> {
	Optional<ContractEntity> findByContractIdAndTenantId(Long contractId, Long tenantId);

	long deleteByContractIdAndTenantId(Long contractId, Long tenantId);

	void deleteByWorkplaceId(Long workplaceId);
}