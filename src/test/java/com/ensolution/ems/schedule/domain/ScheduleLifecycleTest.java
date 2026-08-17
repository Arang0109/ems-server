package com.ensolution.ems.schedule.domain;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.domain.snapshot.BasicInfo;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 측정계획 생애주기 규칙 검증.
 * 진행 축(취소·완료·재개방)과 가시성 축(삭제·복구)이 서로 독립이라는 것이 핵심 계약이다.
 */
class ScheduleLifecycleTest {

	private static Schedule metaWith(ScheduleStatus status) {
		return Schedule.builder()
			.id(1L)
			.tenantId(10L)
			.stackId(100L)
			.teamId(200L)
			.measurementField(MeasurementField.AIR)
			.sampledAt(LocalDate.of(2026, 5, 1))
			.status(status)
			.build();
	}

	@Nested
	@DisplayName("삭제 — 잘못 등록된 계획을 감춘다")
	class Delete {

		@Test
		@DisplayName("측정 예정 상태는 삭제할 수 있고 상태는 보존된다")
		void deletesScheduled() {
			Schedule deleted = metaWith(ScheduleStatus.SCHEDULED).delete(99L);

			assertThat(deleted.isDeleted()).isTrue();
			assertThat(deleted.getDeletedAt()).isNotNull();
			assertThat(deleted.getDeletedBy()).isEqualTo(99L);
			assertThat(deleted.getStatus()).isEqualTo(ScheduleStatus.SCHEDULED);
		}

		@ParameterizedTest(name = "{0}")
		@EnumSource(value = ScheduleStatus.class, names = {"MEASURING", "ANALYZING", "COMPLETED", "CANCELED"})
		@DisplayName("측정에 착수한 이후에는 삭제할 수 없다 — 취소를 사용해야 한다")
		void rejectsAfterMeasurementStarted(ScheduleStatus status) {
			assertThatThrownBy(() -> metaWith(status).delete(99L))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_DELETABLE);
		}

		@Test
		@DisplayName("이미 삭제된 계획은 다시 삭제할 수 없다")
		void rejectsDoubleDelete() {
			Schedule deleted = metaWith(ScheduleStatus.SCHEDULED).delete(99L);

			assertThatThrownBy(() -> deleted.delete(99L))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_ALREADY_DELETED);
		}

		@Test
		@DisplayName("삭제된 계획은 편집할 수 없다")
		void rejectsEditOfDeleted() {
			Schedule deleted = metaWith(ScheduleStatus.SCHEDULED).delete(99L);

			assertThatThrownBy(deleted::requireEditable)
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_ALREADY_DELETED);
		}
	}

	@Nested
	@DisplayName("복구")
	class Restore {

		@Test
		@DisplayName("삭제 시점의 상태를 그대로 회복한다")
		void restoresPreservingStatus() {
			Schedule restored = metaWith(ScheduleStatus.SCHEDULED).delete(99L).restore();

			assertThat(restored.isDeleted()).isFalse();
			assertThat(restored.getDeletedAt()).isNull();
			assertThat(restored.getDeletedBy()).isNull();
			assertThat(restored.getStatus()).isEqualTo(ScheduleStatus.SCHEDULED);
		}

		@Test
		@DisplayName("삭제되지 않은 계획은 복구 대상이 아니다")
		void rejectsRestoreOfActive() {
			assertThatThrownBy(() -> metaWith(ScheduleStatus.SCHEDULED).restore())
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_DELETED);
		}
	}

	@Nested
	@DisplayName("취소 — 업무가 무산된 계획을 기록으로 남긴다")
	class Cancel {

		@ParameterizedTest(name = "{0}")
		@EnumSource(value = ScheduleStatus.class, names = {"SCHEDULED", "MEASURING", "ANALYZING"})
		@DisplayName("진행 중인 어느 단계에서든 취소할 수 있다")
		void cancelsFromAnyActiveStatus(ScheduleStatus status) {
			assertThat(metaWith(status).cancel().getStatus()).isEqualTo(ScheduleStatus.CANCELED);
		}

		@ParameterizedTest(name = "{0}")
		@EnumSource(value = ScheduleStatus.class, names = {"COMPLETED", "CANCELED"})
		@DisplayName("종단 상태는 취소할 수 없다")
		void rejectsTerminal(ScheduleStatus status) {
			assertThatThrownBy(() -> metaWith(status).cancel())
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_INVALID_STATUS_TRANSITION);
		}

		@Test
		@DisplayName("취소된 계획은 삭제로 감출 수 없다 — 취소는 남는 이력이다")
		void canceledIsNotDeletable() {
			assertThatThrownBy(() -> metaWith(ScheduleStatus.CANCELED).delete(99L))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_DELETABLE);
		}
	}

	@Nested
	@DisplayName("완료와 재개방")
	class CompleteAndReopen {

		@Test
		@DisplayName("분석 중인 계획을 완료로 확정한다")
		void completesFromAnalyzing() {
			assertThat(metaWith(ScheduleStatus.ANALYZING).complete().getStatus())
				.isEqualTo(ScheduleStatus.COMPLETED);
		}

		@ParameterizedTest(name = "{0}")
		@EnumSource(value = ScheduleStatus.class, names = {"SCHEDULED", "MEASURING"})
		@DisplayName("분석을 거치지 않고 완료로 건너뛸 수 없다")
		void rejectsSkippingToComplete(ScheduleStatus status) {
			assertThatThrownBy(() -> metaWith(status).complete())
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_INVALID_STATUS_TRANSITION);
		}

		/**
		 * 재개방은 종단을 풀어 시작점으로만 되돌린다.
		 * 실제로 돌아갈 단계는 {@link ScheduleProgress} 가 스냅샷에서 재도출하므로 여기서 정하지 않는다.
		 */
		@ParameterizedTest(name = "{0}")
		@EnumSource(value = ScheduleStatus.class, names = {"COMPLETED", "CANCELED"})
		@DisplayName("종단 상태를 풀면 측정 예정으로 되돌아간다")
		void reopenClearsTerminalStatus(ScheduleStatus status) {
			assertThat(metaWith(status).reopen(true).getStatus()).isEqualTo(ScheduleStatus.SCHEDULED);
		}

		@ParameterizedTest(name = "{0}")
		@EnumSource(value = ScheduleStatus.class, names = {"SCHEDULED", "MEASURING", "ANALYZING"})
		@DisplayName("진행 중인 계획은 되돌릴 것이 없다")
		void rejectsReopenOfActive(ScheduleStatus status) {
			assertThatThrownBy(() -> metaWith(status).reopen(true))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_REOPENABLE);
		}

		@Test
		@DisplayName("완료 재개방은 관리자만 할 수 있다")
		void completedReopenRequiresAdmin() {
			assertThatThrownBy(() -> metaWith(ScheduleStatus.COMPLETED).reopen(false))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_REOPEN_FORBIDDEN);
		}

		/** 실수로 취소하면 입력한 측정 데이터가 잠기므로 담당자가 즉시 되돌릴 수 있어야 한다. */
		@Test
		@DisplayName("취소 철회는 관리자가 아니어도 할 수 있다")
		void canceledReopenDoesNotRequireAdmin() {
			assertThat(metaWith(ScheduleStatus.CANCELED).reopen(false).getStatus())
				.isEqualTo(ScheduleStatus.SCHEDULED);
		}

		@Test
		@DisplayName("재개방한 계획은 다시 편집할 수 있다")
		void reopenedIsEditable() {
			Schedule reopened = metaWith(ScheduleStatus.CANCELED).reopen(false);

			assertThatCode(reopened::requireEditable).doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("재개방 후 단계 재도출")
	class ReopenProgress {

		/** 시료접수일까지 입력된 스냅샷 — 완료 건이 원래 갖고 있는 형태다. */
		private static ScheduleSnapshot analyzedSnapshot() {
			BasicInfo basicInfo = new BasicInfo("REF-001", null, null, null, null,
				LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 2), null, null,
				LocalTime.of(9, 30), null, MeasurementField.AIR, "자가측정용");
			return new ScheduleSnapshot("1", 1L, 10L, ScheduleStatus.COMPLETED,
				basicInfo, null, null, null, null, null, null);
		}

		@Test
		@DisplayName("완료 건은 분석 중으로 돌아간다 — 시트·접수일이 모두 남아 있다")
		void completedReturnsToAnalyzing() {
			Schedule cleared = metaWith(ScheduleStatus.COMPLETED).reopen(true);

			assertThat(ScheduleProgress.advance(cleared, analyzedSnapshot()).getStatus())
				.isEqualTo(ScheduleStatus.ANALYZING);
		}

		@Test
		@DisplayName("아무 데이터도 없이 취소된 건은 측정 예정으로 돌아간다")
		void emptyCanceledReturnsToScheduled() {
			Schedule cleared = metaWith(ScheduleStatus.CANCELED).reopen(false);
			ScheduleSnapshot empty = new ScheduleSnapshot("1", 1L, 10L, ScheduleStatus.CANCELED,
				null, null, null, null, null, null, null);

			assertThat(ScheduleProgress.advance(cleared, empty).getStatus())
				.isEqualTo(ScheduleStatus.SCHEDULED);
		}
	}
}
