package com.ensolution.ems.schedule.domain.snapshot;

/**
 * 측정 시점 고객사(측정대행업체) 스냅샷.
 *
 * <p>상호·사업자번호·대표자·주소는 테넌트 원장의 사본이다. {@code analyst}·{@code technicalManager}도
 * 원장이 기본값을 갖지만 <b>회차마다 달라질 수 있어</b> {@link #withStaff}로 이 문서만 고칠 수 있다 —
 * 측정자 이름을 {@link TeamSnapshot#withMembers}로 회차별로 바꾸는 것과 같은 규약이며,
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
	 * 성적서 서명란에 들어갈 담당자 이름만 교체한 새 스냅샷을 반환한다.
	 * 전달되지 않은(공백 포함) 이름은 기존 값을 유지하며, 원장 연결키({@code tenantId})와
	 * 상호·주소 등 원장 사본은 바꾸지 않는다.
	 */
	public TenantSnapshot withStaff(String analyst, String technicalManager) {
		return new TenantSnapshot(
			tenantId, name, bizNumber, representative, roadAddress, detailAddress, zipcode,
			SnapshotMerge.keepText(analyst, this.analyst),
			SnapshotMerge.keepText(technicalManager, this.technicalManager));
	}
}
