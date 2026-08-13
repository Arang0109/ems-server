package com.ensolution.ems.schedule.domain.snapshot;

import com.ensolution.ems.global.common.enums.Grade;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.Orientation;
import com.ensolution.ems.global.common.enums.Shape;

import java.util.List;

import static com.ensolution.ems.schedule.domain.snapshot.SnapshotMerge.keep;
import static com.ensolution.ems.schedule.domain.snapshot.SnapshotMerge.keepText;

/** 측정 시점 측정시설(굴뚝) 스냅샷. 하위로 배출시설·방지시설 스냅샷을 품는다. */
public record StackSnapshot(
	Long stackId,
	MeasurementField field,
	String name,
	String semsNumber,
	Grade grade,
	String mainProduct,
	Integer standardOxygen,
	Double height,
	Double horizontalLength,
	Double verticalLength,
	Shape shape,
	Orientation orientation,
	List<FacilitySnapshot> facilities,
	List<PreventionSnapshot> preventions
) {
	/**
	 * 전달된 patch의 값으로 필드를 덮어쓴 새 스냅샷을 반환한다.
	 * null(문자열은 공백 포함)인 필드는 기존 값을 유지하며, 원장 연결키인 {@code stackId}는 변경하지 않는다.
	 * {@code facilities}·{@code preventions}는 목록이므로 전달된 경우 전체 교체한다.
	 */
	public StackSnapshot merge(StackSnapshot patch) {
		if (patch == null) return this;
		return new StackSnapshot(
			stackId,
			keep(patch.field(), field),
			keepText(patch.name(), name),
			keepText(patch.semsNumber(), semsNumber),
			keep(patch.grade(), grade),
			keepText(patch.mainProduct(), mainProduct),
			keep(patch.standardOxygen(), standardOxygen),
			keep(patch.height(), height),
			keep(patch.horizontalLength(), horizontalLength),
			keep(patch.verticalLength(), verticalLength),
			keep(patch.shape(), shape),
			keep(patch.orientation(), orientation),
			keep(patch.facilities(), facilities),
			keep(patch.preventions(), preventions));
	}
}
