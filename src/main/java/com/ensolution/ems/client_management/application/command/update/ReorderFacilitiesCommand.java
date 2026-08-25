package com.ensolution.ems.client_management.application.command.update;

import java.util.List;

/**
 * 배출시설 표시 순서 변경 요청.
 *
 * <p>{@code orderedIds} 는 이 측정지점의 배출시설 <b>전체</b>여야 하며, 배열 순서가 곧 표시 순위다.
 */
public record ReorderFacilitiesCommand(
	Long tenantId,
	Long stackId,
	List<Long> orderedIds
) {
}
