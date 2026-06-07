package com.ensolution.ems.client_management.infrastructure.repository;

import com.ensolution.ems.client_management.infrastructure.entity.JpaStackPollutantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaStackPollutantRepository extends JpaRepository<JpaStackPollutantEntity, Long> {

	@Query("SELECT sp FROM JpaStackPollutantEntity sp JOIN FETCH sp.pollutant WHERE sp.stack.id = :stackId")
	List<JpaStackPollutantEntity> findByStackId(@Param("stackId") Long stackId);

	boolean existsByStackIdAndPollutantId(Long stackId, Long pollutantId);
}
