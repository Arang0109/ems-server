package com.ensolution.ems.equipment.application.port.in;

import java.time.LocalDate;
import java.util.List;

/**
 * 장비 정보를 타 모듈에 제공하는 인바운드 포트.
 * 소유권 격리를 위해 tenantId를 함께 받으며, 미존재·타 tenant는 NOT_FOUND로 은닉한다.
 */
public interface EquipmentQueryUseCase {

	EquipmentSummary getEquipmentSummary(String equipmentId, Long tenantId);

	/**
	 * 알림이 켜진 검사 항목 중 다음 예정일이 {@code dueDate} 이하인 건을 임박한 순으로 반환한다.
	 * 사용 가능(ACTIVE) 장비만 대상이며, 이미 기한을 넘긴 항목도 포함한다.
	 * 검사 대상이 아닌 종류와 예정일을 특정할 수 없는 항목(최종 수검일·주기 누락)은 제외한다.
	 * <p>
	 * 한 장비가 여러 검사에서 임박하면 항목별로 여러 건이 반환된다.
	 */
	List<InspectionDueSummary> findInspectionDueBefore(Long tenantId, LocalDate dueDate);
}
