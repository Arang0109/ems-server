package com.ensolution.ems.equipment.application.port.in;

/**
 * 장비 정보를 타 모듈에 제공하는 인바운드 포트.
 * 소유권 격리를 위해 tenantId를 함께 받으며, 미존재·타 tenant는 NOT_FOUND로 은닉한다.
 */
public interface EquipmentQueryUseCase {
	EquipmentSummary getEquipmentSummary(String equipmentId, Long tenantId);
}
