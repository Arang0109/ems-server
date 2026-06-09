package com.ensolution.ems.client_management.domain.port;

import com.ensolution.ems.client_management.application.command.list_item.WorkplaceListItem;
import com.ensolution.ems.client_management.domain.Workplace;

import java.util.List;

public interface WorkplaceRepository {
	Workplace save(Workplace workplace);
	Workplace findById(Long id);
	List<WorkplaceListItem> findByCompanyId(Long companyId);
	List<WorkplaceListItem> findAll();
	void deleteById(Long id);
	
	boolean existsById(Long workplaceId);
	boolean existsByNameAndCompanyId(String name, Long companyId);
}
