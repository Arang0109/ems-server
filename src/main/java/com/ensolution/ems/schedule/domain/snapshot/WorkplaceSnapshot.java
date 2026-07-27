package com.ensolution.ems.schedule.domain.snapshot;

import com.ensolution.ems.global.common.enums.Grade;

import static com.ensolution.ems.schedule.domain.snapshot.SnapshotMerge.keep;
import static com.ensolution.ems.schedule.domain.snapshot.SnapshotMerge.keepText;

/**
 * 측정 시점 사업장 스냅샷. 하위로 측정시설 스냅샷을 품는다.
 * 배출시설관리자·시료채취입회자는 측정계획마다 달라지는 값이므로 여기가 아니라 {@link BasicInfo}가 보유한다
 * (사업장 원장의 값은 측정계획 생성 시 {@code BasicInfo}의 초기값으로만 쓰인다).
 */
public record WorkplaceSnapshot(
	Long workplaceId,
	String name,
	String bizNumber,
	String roadAddress,
	String detailAddress,
	String zipcode,
	Grade grade,
	StackSnapshot stack
) {
	/**
	 * 전달된 patch의 값으로 필드를 덮어쓴 새 스냅샷을 반환한다.
	 * null(문자열은 공백 포함)인 필드는 기존 값을 유지하며, 원장 연결키인 {@code workplaceId}는 변경하지 않는다.
	 * 하위 측정시설은 재귀 병합하고, 기존 측정시설이 없으면 patch의 측정시설을 그대로 채택한다.
	 */
	public WorkplaceSnapshot merge(WorkplaceSnapshot patch) {
		if (patch == null) return this;
		return new WorkplaceSnapshot(
			workplaceId,
			keepText(patch.name(), name),
			keepText(patch.bizNumber(), bizNumber),
			keepText(patch.roadAddress(), roadAddress),
			keepText(patch.detailAddress(), detailAddress),
			keepText(patch.zipcode(), zipcode),
			keep(patch.grade(), grade),
			stack == null ? patch.stack() : stack.merge(patch.stack()));
	}
}
