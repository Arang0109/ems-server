package com.ensolution.ems.contract.infrastructure.repository;

import com.ensolution.ems.contract.infrastructure.entity.ContractEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractJpaRepository extends JpaRepository<ContractEntity, Long> {
	void deleteByWorkplaceId(Long workplaceId);
}