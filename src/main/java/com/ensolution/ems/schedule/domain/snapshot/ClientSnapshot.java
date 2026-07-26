package com.ensolution.ems.schedule.domain.snapshot;

import static com.ensolution.ems.schedule.domain.snapshot.SnapshotMerge.keepText;

/** 측정 시점 의뢰기관 스냅샷. 하위로 사업장 스냅샷을 품는 트리 루트. */
public record ClientSnapshot(
	Long clientId,
	String name,
	String bizNumber,
	String representative,
	String roadAddress,
	String detailAddress,
	String zipcode,

	String facilityManager,
	String samplingWitness,

	String email,
	String tel,
	WorkplaceSnapshot workplace
) {
	/**
	 * 전달된 patch의 값으로 필드를 덮어쓴 새 스냅샷을 반환한다.
	 * null(공백 포함)인 필드는 기존 값을 유지하며, 원장 연결키인 {@code clientId}는 변경하지 않는다.
	 * 하위 사업장은 재귀 병합하고, 기존 사업장이 없으면 patch의 사업장을 그대로 채택한다.
	 */
	public ClientSnapshot merge(ClientSnapshot patch) {
		if (patch == null) return this;
		return new ClientSnapshot(
			clientId,
			keepText(patch.name(), name),
			keepText(patch.bizNumber(), bizNumber),
			keepText(patch.representative(), representative),
			keepText(patch.roadAddress(), roadAddress),
			keepText(patch.detailAddress(), detailAddress),
			keepText(patch.zipcode(), zipcode),
			keepText(patch.facilityManager(), facilityManager),
			keepText(patch.samplingWitness(), samplingWitness),
			keepText(patch.email(), email),
			keepText(patch.tel(), tel),
			workplace == null ? patch.workplace() : workplace.merge(patch.workplace()));
	}
}
