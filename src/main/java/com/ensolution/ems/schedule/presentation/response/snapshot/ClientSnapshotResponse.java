package com.ensolution.ems.schedule.presentation.response.snapshot;

/**
 * 측정 시점 의뢰기관 스냅샷 응답. 사업장→측정시설로 이어지는 트리의 루트다.
 * 배출시설관리자·시료채취입회자 등 측정계획마다 달라지는 담당자는
 * {@link BasicInfoResponse}가 보유한다.
 */
public record ClientSnapshotResponse(
	Long clientId,
	String name,
	String bizNumber,
	String representative,
	String roadAddress,
	String detailAddress,
	String zipcode,

	String email,
	String tel,
	WorkplaceSnapshotResponse workplace
) {}
