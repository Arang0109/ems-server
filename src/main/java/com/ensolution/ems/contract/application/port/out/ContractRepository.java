package com.ensolution.ems.contract.application.port.out;

import com.ensolution.ems.contract.application.command.ContractListItem;
import com.ensolution.ems.contract.domain.Contract;

import java.util.List;

public interface ContractRepository {
	Contract save(Contract contract);
	Contract findById(Long id, Long tenantId);
	List<ContractListItem> findByWorkplaceId(Long workplaceId, Long tenantId);
	List<ContractListItem> findAllByTenantId(Long tenantId);
	void deleteById(Long id, Long tenantId);
	void deleteByWorkplaceId(Long workplaceId);
}
