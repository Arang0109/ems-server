package com.ensolution.ems.client_management.application.port.out;

import com.ensolution.ems.client_management.application.command.list_item.StackPollutantListItem;
import com.ensolution.ems.client_management.domain.StackPollutant;

import java.util.List;

public interface StackPollutantRepository {
	StackPollutant save(StackPollutant stackPollutant);
	List<StackPollutantListItem> findByStackId(Long stackId, Long tenantId);
	void deleteById(Long id, Long tenantId);
	boolean existsByStackIdAndPollutantId(Long stackId, Long pollutantId);
}
