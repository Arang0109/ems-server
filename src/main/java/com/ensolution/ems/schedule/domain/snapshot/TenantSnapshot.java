package com.ensolution.ems.schedule.domain.snapshot;

import static com.ensolution.ems.schedule.domain.snapshot.SnapshotMerge.keepText;

/**
 * 측정 시점 고객사(측정대행업체) 스냅샷.
 *
 * <p>상호·사업자번호·대표자·주소는 테넌트 원장의 사본이다. {@code analyst}·{@code technicalManager}도
 * 원장이 기본값을 갖지만 <b>회차마다 달라질 수 있어</b> {@link #merge}로 이 문서만 고칠 수 있다 —
 * 측정자 이름을 {@link TeamSnapshot#merge}로 회차별로 바꾸는 것과 같은 규약이며,
 * 두 경우 모두 원장은 건드리지 않는다.
 */
public record TenantSnapshot(
	Long tenantId,
	String name,
	String bizNumber,
	String representative,
	String roadAddress,
	String detailAddress,
	String zipcode,

	String analyst,             // 시료분석검사자
	String technicalManager     // 기술책임자
) {

	/**
	 * 전달된 조각을 덮어쓴 새 고객사 스냅샷을 반환한다.
	 * 전달되지 않은(공백 포함) 필드는 기존 값을 유지하는 <b>부분 갱신</b>이다 —
	 * 성적서 서명란 담당자를 현장 채취 탭과 실험·분석 탭이 <b>공유</b>하므로, 자기 것이 아닌 칸에
	 * null을 실어 보내는 호출자가 상대의 입력을 지우지 않아야 한다.
	 * 원장 연결키인 {@code tenantId}는 바꾸지 않는다.
	 */
	public TenantSnapshot merge(TenantSnapshot patch) {
		if (patch == null) return this;
		return new TenantSnapshot(
			tenantId,
			keepText(patch.name(), name),
			keepText(patch.bizNumber(), bizNumber),
			keepText(patch.representative(), representative),
			keepText(patch.roadAddress(), roadAddress),
			keepText(patch.detailAddress(), detailAddress),
			keepText(patch.zipcode(), zipcode),
			keepText(patch.analyst(), analyst),
			keepText(patch.technicalManager(), technicalManager));
	}
}
