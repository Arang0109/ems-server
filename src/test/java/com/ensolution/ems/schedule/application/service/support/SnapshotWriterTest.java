package com.ensolution.ems.schedule.application.service.support;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.FakeScheduleDocumentRepository;
import com.ensolution.ems.schedule.domain.snapshot.SamplingSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 문서 단위 낙관적 락 아래에서의 부분 갱신 규약 검증.
 *
 * <p>측정 시트와 실험분석정보가 한 문서를 공유하게 되면서 여러 저장 경로가 같은 {@code @Version}을
 * 놓고 경합한다. <b>물리 충돌은 다시 읽어 재적용하고, 변경 함수는 재읽은 문서를 기준으로 동작한다</b>는
 * 것이 남의 입력을 되돌리지 않는 유일한 근거이므로 여기서 고정한다.
 */
class SnapshotWriterTest {

	private static final Long TENANT = 1L;
	private static final Long SCHEDULE = 1L;

	private FakeScheduleDocumentRepository documentRepository;
	private SnapshotWriter writer;

	@BeforeEach
	void setUp() {
		documentRepository = new FakeScheduleDocumentRepository();
		writer = new SnapshotWriter(documentRepository);
		documentRepository.given(new ScheduleSnapshot(
			"1", SCHEDULE, TENANT, 0L, null, null, null,
			SamplingSnapshot.create("김담당", "이입회"), List.of()));
	}

	@Test
	@DisplayName("변경 함수를 적용해 저장하고 저장된 스냅샷을 돌려준다")
	void appliesMutationAndSaves() {
		ScheduleSnapshot saved = writer.write(SCHEDULE, TENANT, snapshot ->
			snapshot.withSampling(snapshot.samplingData().update(LocalTime.of(9, 30), null, null, null)));

		assertThat(saved.samplingData().samplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
		assertThat(documentRepository.findByScheduleId(SCHEDULE, TENANT)
			.samplingData().samplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
	}

	@Test
	@DisplayName("저장이 물리적으로 겹치면 문서를 다시 읽어 변경 함수를 재적용한다")
	void retriesOnPhysicalConflict() {
		documentRepository.failNextSaves(1);
		AtomicInteger calls = new AtomicInteger();

		ScheduleSnapshot saved = writer.write(SCHEDULE, TENANT, snapshot -> {
			calls.incrementAndGet();
			return snapshot.withSampling(
				snapshot.samplingData().update(LocalTime.of(9, 30), null, null, null));
		});

		// 변경 함수는 시도마다 다시 호출된다 — 그래야 재읽은 문서를 근거로 적용한다.
		assertThat(calls.get()).isEqualTo(2);
		assertThat(saved.samplingData().samplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
	}

	@Test
	@DisplayName("재시도 한도를 넘으면 사용자에게 충돌을 알린다")
	void failsAfterMaxAttempts() {
		documentRepository.failNextSaves(SnapshotWriter.MAX_ATTEMPTS);
		AtomicInteger calls = new AtomicInteger();

		assertThatThrownBy(() -> writer.write(SCHEDULE, TENANT, snapshot -> {
			calls.incrementAndGet();
			return snapshot;
		}))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_SHEET_VERSION_CONFLICT);

		assertThat(calls.get()).isEqualTo(SnapshotWriter.MAX_ATTEMPTS);
	}

	@Test
	@DisplayName("문서가 없으면 재시도하지 않고 그대로 알린다")
	void propagatesNotFound() {
		assertThatThrownBy(() -> writer.write(999L, TENANT, snapshot -> snapshot))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_DOCUMENT_NOT_FOUND);
	}

	@Test
	@DisplayName("다른 고객사의 문서는 찾지 못한다")
	void isolatesTenant() {
		assertThatThrownBy(() -> writer.write(SCHEDULE, 999L, snapshot -> snapshot))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_DOCUMENT_NOT_FOUND);
	}
}
