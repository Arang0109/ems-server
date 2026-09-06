package com.ensolution.ems.schedule.presentation.response.snapshot;

/**
 * 측정 시점 고객사 스냅샷 응답.
 * {@code analyst}·{@code technicalManager}는 성적서 서명란 담당자로, 고객사 원장의 기본값을
 * 복사해 두되 회차별로 다르면 이 문서만 바뀐다.
 */
public record TenantSnapshotResponse(
	Long tenantId,
	String name,
	String bizNumber,
	String representative,
	String roadAddress,
	String detailAddress,
	String zipcode,
	String analyst,
	String technicalManager
) {}
