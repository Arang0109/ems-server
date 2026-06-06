package com.ensolution.ems.client_management.application.port;

import com.ensolution.ems.global.common.enums.MeasurementField;

import java.util.List;
import java.util.Map;

public interface WorkplaceQueryUseCase {
	ContractSummary getSummaryById(Long workplaceId);
	Map<Long, List<MeasurementField>> getFieldsByWorkplaceIds(List<Long> workplaceIds);
	boolean existsById(Long workplaceId);
}