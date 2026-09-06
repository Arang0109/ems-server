package com.ensolution.ems.schedule.application;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.port.out.ScheduleDocumentRepository;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 인메모리 {@link ScheduleDocumentRepository}.
 *
 * <p>실제 어댑터와 마찬가지로 <b>문서 단위 낙관적 락을 흉내 낸다</b> — 읽어온 version과 저장 대상의
 * version이 어긋나면 {@link OptimisticLockingFailureException}을 던지고, 저장에 성공하면 version을
 * 1 올린다. 측정 시트와 실험분석정보가 한 문서를 공유하게 되면서 재시도 규약({@code SnapshotWriter})이
 * 이 모듈의 핵심 불변식이 되었는데, 저장이 무조건 성공하는 Fake로는 그것을 테스트로 고정할 수 없다.
 */
public class FakeScheduleDocumentRepository implements ScheduleDocumentRepository {

	private final List<ScheduleSnapshot> snapshots = new ArrayList<>();

	/** 다음 몇 번의 저장을 물리 충돌로 실패시킬지. 재시도 경로를 고정하는 데 쓴다. */
	private int failNextSaves = 0;

	/** 픽스처 등록용. 저장 규칙(version 검사)을 거치지 않고 그대로 심는다. */
	public void given(ScheduleSnapshot snapshot) {
		snapshots.removeIf(s -> Objects.equals(s.scheduleId(), snapshot.scheduleId()));
		snapshots.add(snapshot);
	}

	/** 다음 {@code count}번의 저장이 물리 충돌로 실패하게 한다. */
	public void failNextSaves(int count) {
		this.failNextSaves = count;
	}

	@Override
	public ScheduleSnapshot save(ScheduleSnapshot snapshot) {
		if (failNextSaves > 0) {
			failNextSaves--;
			throw new OptimisticLockingFailureException("simulated document version conflict");
		}

		ScheduleSnapshot current = find(snapshot.scheduleId(), snapshot.tenantId());
		if (current != null && !Objects.equals(current.version(), snapshot.version())) {
			throw new OptimisticLockingFailureException(
				"version mismatch: expected " + current.version() + " but was " + snapshot.version());
		}

		ScheduleSnapshot saved = withVersion(snapshot, snapshot.version() == null ? 0L : snapshot.version() + 1);
		snapshots.removeIf(s -> Objects.equals(s.scheduleId(), saved.scheduleId()));
		snapshots.add(saved);
		return saved;
	}

	@Override
	public ScheduleSnapshot findByScheduleId(Long scheduleId, Long tenantId) {
		ScheduleSnapshot found = find(scheduleId, tenantId);
		if (found == null) throw new CustomException(ErrorCode.SCHEDULE_DOCUMENT_NOT_FOUND);
		return found;
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

	private ScheduleSnapshot find(Long scheduleId, Long tenantId) {
		return snapshots.stream()
			.filter(s -> Objects.equals(s.scheduleId(), scheduleId))
			.filter(s -> Objects.equals(s.tenantId(), tenantId))
			.findFirst()
			.orElse(null);
	}

	private static ScheduleSnapshot withVersion(ScheduleSnapshot snapshot, Long version) {
		return new ScheduleSnapshot(
			snapshot.id(), snapshot.scheduleId(), snapshot.tenantId(), version,
			snapshot.client(), snapshot.tenant(), snapshot.team(), snapshot.samplingData(), snapshot.items());
	}
}
