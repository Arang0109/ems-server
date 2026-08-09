package com.ensolution.ems.equipment.domain;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * 장비의 검사 항목 정규화·부분 갱신·검사 실시 반영을 검증한다.
 * 특히 "요청에 없는 검사 종류는 손대지 않는다"는 규칙이 이번 구조의 핵심 회귀 지점이다.
 */
class EquipmentInspectionTest {

	private Equipment register(List<InspectionItem> inspections) {
		return Equipment.register(1L, EquipType.GAS_SAMPLER, "GS-001", "SN-1", "모델", "가스채취기",
			"별칭", null, "제조사", "한국", null, null, inspections, null);
	}

	private InspectionItemChange change(InspectionType type, Boolean enabled, Integer cycleMonths) {
		return new InspectionItemChange(type, enabled, cycleMonths, null, null, null);
	}

	@Test
	void 검사_항목을_주지_않아도_전_종류가_채워진다() {
		Equipment equipment = register(null);

		assertThat(equipment.getInspections())
			.extracting(InspectionItem::type, InspectionItem::enabled)
			.containsExactly(
				tuple(InspectionType.PRECISION_INSPECTION, false),
				tuple(InspectionType.CALIBRATION, false),
				tuple(InspectionType.GENERAL_TEST, false));
	}

	@Test
	void 일부_종류만_지정해도_나머지는_비활성으로_채워진다() {
		Equipment equipment = register(List.of(InspectionItem.enabled(InspectionType.CALIBRATION, 12)));

		assertThat(equipment.getInspections()).hasSize(3);
		assertThat(equipment.inspectionOf(InspectionType.CALIBRATION).enabled()).isTrue();
		assertThat(equipment.inspectionOf(InspectionType.GENERAL_TEST).enabled()).isFalse();
	}

	@Test
	void 검사_개념_도입_이전_장비도_전_종류가_채워진다() {
		Equipment legacy = Equipment.builder().tenantId(1L).type(EquipType.NOZZLE).build();   // inspections == null

		assertThat(legacy.getInspections())
			.extracting(InspectionItem::type)
			.containsExactly(InspectionType.PRECISION_INSPECTION, InspectionType.CALIBRATION,
				InspectionType.GENERAL_TEST);
		assertThat(legacy.getInspections()).allMatch(item -> !item.enabled());
	}

	@Test
	void 수정_요청에_검사가_없으면_설정이_유지된다() {
		Equipment equipment = register(List.of(
			new InspectionItem(InspectionType.CALIBRATION, true, 12, LocalDate.of(2025, 3, 1), null, true)));

		Equipment updated = equipment.update(null, null, null, null, null, "별칭변경",
			null, null, null, null, null, null, null);

		InspectionItem calibration = updated.inspectionOf(InspectionType.CALIBRATION);
		assertThat(calibration.enabled()).isTrue();
		assertThat(calibration.cycleMonths()).isEqualTo(12);
		assertThat(calibration.lastInspectedAt()).isEqualTo(LocalDate.of(2025, 3, 1));
	}

	@Test
	void 전달한_종류만_갱신되고_나머지는_그대로다() {
		Equipment equipment = register(List.of(
			new InspectionItem(InspectionType.CALIBRATION, true, 12, LocalDate.of(2025, 3, 1), null, true),
			new InspectionItem(InspectionType.PRECISION_INSPECTION, true, 24, LocalDate.of(2024, 9, 1), null, true)));

		Equipment updated = equipment.update(null, null, null, null, null, null, null, null, null, null, null,
			List.of(change(InspectionType.CALIBRATION, null, 6)), null);

		InspectionItem calibration = updated.inspectionOf(InspectionType.CALIBRATION);
		assertThat(calibration.cycleMonths()).isEqualTo(6);
		// 주기만 바꿨을 뿐인데 수검 이력이 날아가면 안 된다
		assertThat(calibration.lastInspectedAt()).isEqualTo(LocalDate.of(2025, 3, 1));

		InspectionItem precision = updated.inspectionOf(InspectionType.PRECISION_INSPECTION);
		assertThat(precision.cycleMonths()).isEqualTo(24);
		assertThat(precision.lastInspectedAt()).isEqualTo(LocalDate.of(2024, 9, 1));
	}

	@Test
	void 알림만_꺼도_검사_대상_여부는_그대로다() {
		Equipment equipment = register(List.of(InspectionItem.enabled(InspectionType.CALIBRATION, 12)));

		Equipment updated = equipment.update(null, null, null, null, null, null, null, null, null, null, null,
			List.of(new InspectionItemChange(InspectionType.CALIBRATION, null, null, null, null, false)), null);

		InspectionItem calibration = updated.inspectionOf(InspectionType.CALIBRATION);
		assertThat(calibration.enabled()).isTrue();
		assertThat(calibration.notificationEnabled()).isFalse();
		assertThat(calibration.notifiable()).isFalse();
	}

	@Test
	void 검사_대상에서_빼면_예정일이_사라진다() {
		Equipment equipment = register(List.of(
			new InspectionItem(InspectionType.CALIBRATION, true, 12, LocalDate.of(2025, 3, 1), null, true)));

		Equipment updated = equipment.update(null, null, null, null, null, null, null, null, null, null, null,
			List.of(change(InspectionType.CALIBRATION, false, null)), null);

		assertThat(updated.inspectionOf(InspectionType.CALIBRATION).nextDueDate()).isNull();
		// 주기·수검일은 남아 있으므로 다시 켜면 그대로 복구된다
		assertThat(updated.inspectionOf(InspectionType.CALIBRATION).lastInspectedAt())
			.isEqualTo(LocalDate.of(2025, 3, 1));
	}

	@Test
	void 검사_실시를_반영하면_해당_종류만_갱신된다() {
		Equipment equipment = register(List.of(
			InspectionItem.enabled(InspectionType.CALIBRATION, 6),
			InspectionItem.enabled(InspectionType.PRECISION_INSPECTION, 24)));

		Equipment updated = equipment.recordInspection(
			InspectionType.CALIBRATION, LocalDate.of(2026, 8, 1), null);

		assertThat(updated.inspectionOf(InspectionType.CALIBRATION).nextDueDate())
			.isEqualTo(LocalDate.of(2027, 2, 1));
		assertThat(updated.inspectionOf(InspectionType.PRECISION_INSPECTION).lastInspectedAt()).isNull();
	}

	@Test
	void 검사_대상이_아닌_종류는_실시_기록을_남길_수_없다() {
		Equipment equipment = register(List.of(InspectionItem.enabled(InspectionType.CALIBRATION, 12)));

		assertThatThrownBy(() -> equipment.recordInspection(
			InspectionType.GENERAL_TEST, LocalDate.of(2026, 8, 1), null))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.EQUIPMENT_INSPECTION_DISABLED);
	}

	@Test
	void 알림_대상_중_기한_이내_항목만_임박한_순으로_반환된다() {
		Equipment equipment = register(List.of(
			// 이미 기한 초과 (2025-09-01)
			new InspectionItem(InspectionType.CALIBRATION, true, 6, LocalDate.of(2025, 3, 1), null, true),
			// 기한 이내지만 알림 꺼짐
			new InspectionItem(InspectionType.PRECISION_INSPECTION, true, 12, LocalDate.of(2025, 9, 1), null, false),
			// 검사 대상 아님
			new InspectionItem(InspectionType.GENERAL_TEST, false, 6, LocalDate.of(2025, 5, 1), null, true)));

		List<InspectionItem> due = equipment.notifiableItemsDueBefore(LocalDate.of(2026, 10, 1));

		assertThat(due)
			.extracting(InspectionItem::type)
			.containsExactly(InspectionType.CALIBRATION);
	}
}
