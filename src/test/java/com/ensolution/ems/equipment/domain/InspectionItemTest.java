package com.ensolution.ems.equipment.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** 검사 항목의 다음 예정일 계산과 알림 대상 판정을 검증한다. */
class InspectionItemTest {

	private InspectionItem item(boolean enabled, Integer cycleMonths, LocalDate lastInspectedAt, LocalDate override) {
		return new InspectionItem(InspectionType.CALIBRATION, enabled, cycleMonths, lastInspectedAt, override, true);
	}

	@Test
	void 검사_대상이_아니면_예정일이_없다() {
		InspectionItem item = item(false, 12, LocalDate.of(2025, 3, 1), null);

		assertThat(item.nextDueDate()).isNull();
	}

	@Test
	void 최종_수검일에_주기를_더해_예정일을_구한다() {
		InspectionItem item = item(true, 12, LocalDate.of(2025, 3, 1), null);

		assertThat(item.nextDueDate()).isEqualTo(LocalDate.of(2026, 3, 1));
	}

	@Test
	void 성적서_유효기간이_지정되면_주기_계산보다_우선한다() {
		InspectionItem item = item(true, 12, LocalDate.of(2025, 3, 1), LocalDate.of(2026, 12, 31));

		assertThat(item.nextDueDate()).isEqualTo(LocalDate.of(2026, 12, 31));
	}

	@Test
	void 최종_수검일이_없으면_예정일을_특정할_수_없다() {
		assertThat(item(true, 12, null, null).nextDueDate()).isNull();
	}

	@Test
	void 주기가_없거나_0이하면_예정일을_특정할_수_없다() {
		assertThat(item(true, null, LocalDate.of(2025, 3, 1), null).nextDueDate()).isNull();
		assertThat(item(true, 0, LocalDate.of(2025, 3, 1), null).nextDueDate()).isNull();
	}

	@Test
	void 검사_실시를_반영하면_최종_수검일이_갱신된다() {
		InspectionItem inspected = item(true, 12, LocalDate.of(2025, 3, 1), null)
			.inspected(LocalDate.of(2026, 8, 1), null);

		assertThat(inspected.lastInspectedAt()).isEqualTo(LocalDate.of(2026, 8, 1));
		assertThat(inspected.nextDueDate()).isEqualTo(LocalDate.of(2027, 8, 1));
	}

	@Test
	void 유효기간_없이_검사를_기록하면_기존_예정일_지정이_해제된다() {
		InspectionItem inspected = item(true, 12, LocalDate.of(2025, 3, 1), LocalDate.of(2026, 12, 31))
			.inspected(LocalDate.of(2026, 8, 1), null);

		assertThat(inspected.nextDueDateOverride()).isNull();
		assertThat(inspected.nextDueDate()).isEqualTo(LocalDate.of(2027, 8, 1));
	}

	@Test
	void 검사_대상이면서_알림이_켜져야_알림_대상이다() {
		assertThat(new InspectionItem(InspectionType.CALIBRATION, true, 12, null, null, true).notifiable()).isTrue();
		assertThat(new InspectionItem(InspectionType.CALIBRATION, true, 12, null, null, false).notifiable()).isFalse();
		assertThat(new InspectionItem(InspectionType.CALIBRATION, false, 12, null, null, true).notifiable()).isFalse();
		assertThat(new InspectionItem(InspectionType.CALIBRATION, false, 12, null, null, false).notifiable()).isFalse();
	}
}
