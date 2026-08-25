package com.ensolution.ems.schedule.application;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.port.out.ScheduleDocumentRepository;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 인메모리 {@link ScheduleDocumentRepository}. */
public class FakeScheduleDocumentRepository implements ScheduleDocumentRepository {

	private final List<ScheduleSnapshot> snapshots = new ArrayList<>();

	@Override
	public ScheduleSnapshot save(ScheduleSnapshot snapshot) {
		snapshots.removeIf(s -> Objects.equals(s.scheduleId(), snapshot.scheduleId()));
		snapshots.add(snapshot);
		return snapshot;
	}

	@Override
	public ScheduleSnapshot findByScheduleId(Long scheduleId, Long tenantId) {
		return snapshots.stream()
			.filter(s -> Objects.equals(s.scheduleId(), scheduleId))
			.filter(s -> Objects.equals(s.tenantId(), tenantId))
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_DOCUMENT_NOT_FOUND));
	}

	@Override
	public List<ScheduleSnapshot> findAll(Long tenantId) {
		return snapshots.stream().filter(s -> Objects.equals(s.tenantId(), tenantId)).toList();
	}

	@Override
	public void deleteByScheduleId(Long scheduleId, Long tenantId) {
		snapshots.removeIf(s ->
			Objects.equals(s.scheduleId(), scheduleId) && Objects.equals(s.tenantId(), tenantId));
	}
}
