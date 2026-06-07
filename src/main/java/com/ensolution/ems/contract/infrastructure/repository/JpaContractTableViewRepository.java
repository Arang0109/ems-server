package com.ensolution.ems.contract.infrastructure.repository;

import com.ensolution.ems.contract.infrastructure.entity.JpaContractTableViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaContractTableViewRepository extends JpaRepository<JpaContractTableViewEntity, Long> {
	List<JpaContractTableViewEntity> findByWorkplaceId(Long workplaceId);
}