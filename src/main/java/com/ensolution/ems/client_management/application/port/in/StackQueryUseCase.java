package com.ensolution.ems.client_management.application.port.in;

import java.util.List;

/**
 * 측정 대상(측정시설과 상·하위 트리, 시설별 측정항목) 정보를 타 모듈에 제공하는 인바운드 포트.
 * 소유권 격리를 위해 tenantId를 함께 받으며, 미존재·타 tenant는 NOT_FOUND로 은닉한다.
 */
public interface StackQueryUseCase {
	StackMeasurementSummary getMeasurementTargetSummary(Long stackId, Long tenantId);

	/** 테넌트 소속 측정시설(굴뚝) 수. */
	long countStacks(Long tenantId);

	/**
	 * 측정항목을 사업장·측정시설 이름과 함께 평면 목록으로 조회한다.
	 * {@code workplaceId}·{@code stackId}는 선택 필터이며 둘 다 null이면 테넌트 전체를 반환한다.
	 *
	 * <p>주기 이행 현황판의 <b>행 축</b>을 공급한다 — 한 번도 측정하지 않은 항목도 행으로 나와야 하므로,
	 * 축은 이행 이력이 아니라 원장인 이 모듈에서 와야 한다.
	 * {@link #getMeasurementTargetSummary}는 시설 하나의 트리를 통째로 조립하므로 여러 시설을 훑을 때
	 * 쓰면 N+1이 된다. 그래서 목록 전용 계약을 따로 둔다.
	 */
	List<StackMeasurementItemSummary> findMeasurementItems(Long tenantId, Long workplaceId, Long stackId);
}
