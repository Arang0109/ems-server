package com.ensolution.ems.client_management.infrastructure.repository;

import com.ensolution.ems.client_management.infrastructure.entity.PollutantCatalogEntity;
import com.ensolution.ems.global.common.enums.MeasurementField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** 전역 마스터이므로 tenant 조건을 걸지 않는다. */
@Repository
public interface PollutantCatalogJpaRepository extends JpaRepository<PollutantCatalogEntity, Long> {

	/** code는 측정분야 안에서만 유일하므로 field와 함께 조회한다. */
	Optional<PollutantCatalogEntity> findByFieldAndCode(MeasurementField field, String code);

	boolean existsByFieldAndCode(MeasurementField field, String code);

	List<PollutantCatalogEntity> findAllByOrderBySortOrderAscNameKrAsc();

	List<PollutantCatalogEntity> findAllByActiveTrueOrderBySortOrderAscNameKrAsc();

	List<PollutantCatalogEntity> findAllByActiveTrueAndFieldOrderBySortOrderAscNameKrAsc(MeasurementField field);

	List<PollutantCatalogEntity> findAllByFieldOrderBySortOrderAscNameKrAsc(MeasurementField field);
}
