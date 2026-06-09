package com.ensolution.ems.client_management.infrastructure.repository;

import com.ensolution.ems.client_management.infrastructure.entity.StackPollutantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StackPollutantJpaRepository extends JpaRepository<StackPollutantEntity, Long> {

	@Query("SELECT sp FROM StackPollutantEntity sp JOIN FETCH sp.pollutant WHERE sp.stack.id = :stackId")
	List<StackPollutantEntity> findByStackId(@Param("stackId") Long stackId);

	boolean existsByStackIdAndPollutantId(Long stackId, Long pollutantId);
}
