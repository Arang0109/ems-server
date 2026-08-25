package com.ensolution.ems.schedule.infrastructure.adapter;

import com.ensolution.ems.schedule.application.port.out.MeasurementRecordRepository;
import com.ensolution.ems.schedule.domain.history.MeasurementRecord;
import com.ensolution.ems.schedule.infrastructure.mapper.MeasurementRecordEntityMapper;
import com.ensolution.ems.schedule.infrastructure.repository.MeasurementRecordJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional
public class MeasurementRecordRepositoryAdapter implements MeasurementRecordRepository {

	private final MeasurementRecordJpaRepository measurementRecordJpaRepository;
	private final MeasurementRecordEntityMapper mapper;

	@Override
	public List<MeasurementRecord> saveAll(List<MeasurementRecord> records) {
		if (records == null || records.isEmpty()) return List.of();
		return mapper.toDomainList(measurementRecordJpaRepository.saveAll(mapper.toEntities(records)));
	}

	@Override
	@Transactional(readOnly = true)
	public List<MeasurementRecord> findByStackId(Long stackId, Long tenantId) {
		return mapper.toDomainList(
			measurementRecordJpaRepository.findByTenantIdAndStackIdOrderBySampledAtDescRecordIdAsc(
				tenantId, stackId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<MeasurementRecord> findByStackIdAndYear(Long stackId, Long tenantId, int year) {
		return mapper.toDomainList(
			measurementRecordJpaRepository.findByTenantIdAndStackIdAndPeriodYearOrderBySampledAtDescRecordIdAsc(
				tenantId, stackId, year));
	}

	@Override
	@Transactional(readOnly = true)
	public List<MeasurementRecord> findByTenantIdAndYear(Long tenantId, int year) {
		return mapper.toDomainList(measurementRecordJpaRepository.findByTenantIdAndPeriodYear(tenantId, year));
	}

	/** 지울 기록이 없는 경우(주기 도입 이전 계획의 재개방 등)는 정상이므로 예외로 만들지 않는다. */
	@Override
	public void deleteByScheduleId(Long scheduleId, Long tenantId) {
		measurementRecordJpaRepository.deleteByScheduleIdAndTenantId(scheduleId, tenantId);
	}
}
