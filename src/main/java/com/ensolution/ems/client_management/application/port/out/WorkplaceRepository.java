package com.ensolution.ems.client_management.application.port.out;

import com.ensolution.ems.client_management.application.command.list_item.WorkplaceListItem;
import com.ensolution.ems.client_management.domain.Workplace;

import java.util.List;

public interface WorkplaceRepository {
	Workplace save(Workplace workplace);
	Workplace findById(Long id, Long tenantId);
	List<WorkplaceListItem> findByClientId(Long clientId, Long tenantId);
	List<WorkplaceListItem> findAll(Long tenantId);
	void deleteById(Long id, Long tenantId);

	boolean existsById(Long workplaceId, Long tenantId);
	boolean existsByNameAndClientId(String name, Long clientId);
}
