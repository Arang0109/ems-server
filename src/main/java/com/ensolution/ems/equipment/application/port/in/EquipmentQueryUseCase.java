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
	 * 다음 교정 예정일이 {@code dueDate} 이하인 사용 가능(ACTIVE) 장비를 임박한 순으로 반환한다.
	 * 이미 교정 기한을 넘긴 장비도 포함하며, 예정일을 특정할 수 없는 장비(최종 교정일·주기 누락)는 제외한다.
	 */
	List<CalibrationDueSummary> findCalibrationDueBefore(Long tenantId, LocalDate dueDate);
}
