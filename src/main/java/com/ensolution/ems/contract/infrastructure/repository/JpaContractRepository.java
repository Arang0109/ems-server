package com.ensolution.ems.contract.infrastructure.repository;

import com.ensolution.ems.contract.infrastructure.ContractQueryRow;
import com.ensolution.ems.contract.infrastructure.entity.JpaContractEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaContractRepository extends JpaRepository<JpaContractEntity, Long> {

	@Query("""
		SELECT new com.ensolution.ems.contract.infrastructure.ContractQueryRow(
			c.id, c.workplaceId, c.contractName, c.contractDate, c.startDate, c.completionDate,
			c.contractAccount, c.contractAmountUnit, w.company.name, w.name
		)
		FROM JpaContractEntity c
		JOIN JpaWorkplaceEntity w ON c.workplaceId = w.id
		""")
	List<ContractQueryRow> findAllWithWorkplace();

	@Query("""
		SELECT new com.ensolution.ems.contract.infrastructure.ContractQueryRow(
			c.id, c.workplaceId, c.contractName, c.contractDate, c.startDate, c.completionDate,
			c.contractAccount, c.contractAmountUnit, w.company.name, w.name
		)
		FROM JpaContractEntity c
		JOIN JpaWorkplaceEntity w ON c.workplaceId = w.id
		WHERE c.workplaceId = :workplaceId
		""")
	List<ContractQueryRow> findByWorkplaceId(@Param("workplaceId") Long workplaceId);
}
