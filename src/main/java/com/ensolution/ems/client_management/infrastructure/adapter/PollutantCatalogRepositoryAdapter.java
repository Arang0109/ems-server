package com.ensolution.ems.client_management.infrastructure.adapter;

import com.ensolution.ems.client_management.application.port.out.PollutantCatalogRepository;
import com.ensolution.ems.client_management.domain.PollutantCatalog;
import com.ensolution.ems.client_management.infrastructure.mapper.PollutantCatalogEntityMapper;
import com.ensolution.ems.client_management.infrastructure.repository.PollutantCatalogJpaRepository;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional
public class PollutantCatalogRepositoryAdapter implements PollutantCatalogRepository {

	private final PollutantCatalogJpaRepository jpaPollutantCatalogRepository;
	private final PollutantCatalogEntityMapper mapper;

	@Override
	public PollutantCatalog save(PollutantCatalog catalog) {
		return mapper.toDomain(jpaPollutantCatalogRepository.save(mapper.toEntity(catalog)));
	}

	@Override
	@Transactional(readOnly = true)
	public PollutantCatalog findById(Long id) {
		return jpaPollutantCatalogRepository.findById(id)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.POLLUTANT_CATALOG_NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public PollutantCatalog findByFieldAndCode(MeasurementField field, String code) {
		return jpaPollutantCatalogRepository.findByFieldAndCode(field, code)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.POLLUTANT_CATALOG_NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<PollutantCatalog> findAll(MeasurementField field) {
		if (field == null) {
			return mapper.toDomainList(jpaPollutantCatalogRepository.findAllByOrderBySortOrderAscNameKrAsc());
		}
		return mapper.toDomainList(jpaPollutantCatalogRepository.findAllByFieldOrderBySortOrderAscNameKrAsc(field));
	}

	@Override
	@Transactional(readOnly = true)
	public List<PollutantCatalog> findAllActive(MeasurementField field) {
		if (field == null) {
			return mapper.toDomainList(jpaPollutantCatalogRepository.findAllByActiveTrueOrderBySortOrderAscNameKrAsc());
		}
		return mapper.toDomainList(
			jpaPollutantCatalogRepository.findAllByActiveTrueAndFieldOrderBySortOrderAscNameKrAsc(field));
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByFieldAndCode(MeasurementField field, String code) {
		return jpaPollutantCatalogRepository.existsByFieldAndCode(field, code);
	}
}
