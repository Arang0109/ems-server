package com.ensolution.ems.client_management.application.port.out;

import com.ensolution.ems.client_management.application.command.list_item.StackPollutantListItem;
import com.ensolution.ems.client_management.application.port.in.StackMeasurementItemSummary;
import com.ensolution.ems.client_management.domain.StackPollutant;

import java.util.List;

public interface StackPollutantRepository {
	StackPollutant save(StackPollutant stackPollutant);
	StackPollutant findById(Long id, Long tenantId);
	List<StackPollutantListItem> findByStackId(Long stackId, Long tenantId);
	void deleteById(Long id, Long tenantId);
	boolean existsByStackIdAndPollutantId(Long stackId, Long pollutantId);

	/**
	 * 측정항목을 사업장·측정시설 이름과 함께 평면 목록으로 조회한다. {@code workplaceId}·{@code stackId}는
	 * 선택 필터이며 둘 다 null이면 테넌트 전체를 반환한다.
	 *
	 * <p>반환 타입이 {@code port/in} VO인 것은 이 조회의 결과 표현이 모듈 안팎에서 같기 때문이다.
	 * 필드가 동일한 내부 VO를 따로 두면 변환 코드만 늘고 한쪽만 고치는 사고가 난다.
	 * {@code StackMeasurementSummary}도 {@code port/in}에 있으면서 모듈 내부 조립에 그대로 쓰인다.
	 */
	List<StackMeasurementItemSummary> findMeasurementItems(Long tenantId, Long workplaceId, Long stackId);
}
