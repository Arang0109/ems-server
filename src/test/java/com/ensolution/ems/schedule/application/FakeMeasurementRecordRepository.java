package com.ensolution.ems.schedule.application;

import com.ensolution.ems.schedule.application.port.out.MeasurementRecordRepository;
import com.ensolution.ems.schedule.domain.history.MeasurementRecord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** 인메모리 {@link MeasurementRecordRepository}. */
public class FakeMeasurementRecordRepository implements MeasurementRecordRepository {

	private final List<MeasurementRecord> records = new ArrayList<>();
	private long nextId = 1L;

	public List<MeasurementRecord> all() {
		return List.copyOf(records);
	}

	@Override
	public List<MeasurementRecord> saveAll(List<MeasurementRecord> newRecords) {
		List<MeasurementRecord> saved = new ArrayList<>();
		for (MeasurementRecord record : newRecords) {
			MeasurementRecord withId = record.id() == null ? withId(record, nextId++) : record;
			records.add(withId);
			saved.add(withId);
		}
		return saved;
	}

	@Override
	public List<MeasurementRecord> findByStackId(Long stackId, Long tenantId) {
		return records.stream()
			.filter(record -> Objects.equals(record.stackId(), stackId))
			.filter(record -> Objects.equals(record.tenantId(), tenantId))
			.sorted(Comparator.comparing(MeasurementRecord::sampledAt).reversed())
			.toList();
	}

	@Override
	public List<MeasurementRecord> findByStackIdAndYear(Long stackId, Long tenantId, int year) {
		return findByStackId(stackId, tenantId).stream()
			.filter(record -> record.periodYear() == year)
			.toList();
	}

	@Override
	public List<MeasurementRecord> findByTenantIdAndYear(Long tenantId, int year) {
		return records.stream()
			.filter(record -> Objects.equals(record.tenantId(), tenantId))
			.filter(record -> record.periodYear() == year)
			.toList();
	}

	@Override
	public void deleteByScheduleId(Long scheduleId, Long tenantId) {
		records.removeIf(record ->
			Objects.equals(record.scheduleId(), scheduleId) && Objects.equals(record.tenantId(), tenantId));
	}

	private MeasurementRecord withId(MeasurementRecord record, long id) {
		return new MeasurementRecord(
			id, record.tenantId(), record.stackId(), record.scheduleId(),
			record.stackPollutantId(), record.pollutantId(), record.pollutantCode(), record.pollutantNameKr(),
			record.cycle(), record.periodYear(), record.periodIndex(),
			record.sampledAt(), record.completedAt(), record.result());
	}
}
