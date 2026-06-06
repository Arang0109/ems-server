package com.ensolution.ems.client_management.infrastructure.repository;

import com.ensolution.ems.client_management.infrastructure.entity.JpaStackEntity;
import com.ensolution.ems.global.common.enums.MeasurementField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaStackRepository extends JpaRepository<JpaStackEntity, Long> {
    List<JpaStackEntity> findByWorkplaceId(Long workplaceId);
	boolean existsByNameAndWorkplaceIdAndField(String name, Long workplaceId, MeasurementField field);

	@Query("SELECT s FROM JpaStackEntity s JOIN FETCH s.workplace w WHERE w.id IN :workplaceIds")
	List<JpaStackEntity> findByWorkplaceIds(@Param("workplaceIds") List<Long> workplaceIds);
}
