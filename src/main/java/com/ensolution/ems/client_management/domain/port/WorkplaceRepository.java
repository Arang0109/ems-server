package com.ensolution.ems.client_management.domain.port;

import com.ensolution.ems.client_management.application.command.WorkplaceListItem;
import com.ensolution.ems.client_management.domain.Workplace;

import java.util.List;

public interface WorkplaceRepository {
	Workplace save(Workplace workplace);
	Workplace findById(Long id);
	List<WorkplaceListItem> findByCompanyId(Long companyId);
	void deleteById(Long id);
	boolean existsByNameAndCompanyId(String name, Long companyId);
}
