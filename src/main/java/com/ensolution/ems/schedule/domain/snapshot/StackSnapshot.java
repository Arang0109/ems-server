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
	 * <p>
	 * <b>{@code standardOxygen}은 예외로 전달값을 그대로 채택한다.</b> 기준산소농도는 "미적용"이 유효한 값이라
	 * null을 "미전달"로 해석하면 한번 설정한 뒤로는 해제할 방법이 없어진다.
	 * 측정시설(patch)을 통째로 전달하지 않으면 위의 이른 반환에 걸려 아무것도 바뀌지 않으므로,
	 * 측정시설을 전달했다는 것은 기준산소농도까지 그 값으로 확정하겠다는 뜻으로 본다.
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
			patch.standardOxygen(),
			keep(patch.height(), height),
			keep(patch.horizontalLength(), horizontalLength),
			keep(patch.verticalLength(), verticalLength),
			keep(patch.shape(), shape),
			keep(patch.orientation(), orientation),
			keep(patch.facilities(), facilities),
			keep(patch.preventions(), preventions));
	}
}
