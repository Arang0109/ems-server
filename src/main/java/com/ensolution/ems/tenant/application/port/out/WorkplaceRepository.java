package com.ensolution.ems.tenant.application.port.out;

import com.ensolution.ems.tenant.application.command.list_item.WorkplaceListItem;
import com.ensolution.ems.tenant.domain.Workplace;

import java.util.List;

public interface WorkplaceRepository {
	Workplace save(Workplace workplace);
	Workplace findById(Long id, Long tenantId);
	List<WorkplaceListItem> findByClientId(Long clientId, Long tenantId);
	List<WorkplaceListItem> findAll(Long tenantId);
	void deleteById(Long id, Long tenantId);

	boolean existsById(Long workplaceId);
	boolean existsByNameAndClientId(String name, Long clientId);
}
