package com.ensolution.ems.contract.infrastructure.repository;

import com.ensolution.ems.contract.infrastructure.entity.JpaContractEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaContractRepository extends JpaRepository<JpaContractEntity, Long> {
}
