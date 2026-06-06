package com.ensolution.ems.contract.domain.port;

import com.ensolution.ems.contract.application.command.ContractListItem;
import com.ensolution.ems.contract.domain.Contract;

import java.util.List;

public interface ContractRepository {
	Contract save(Contract contract);
	Contract findById(Long id);
	List<ContractListItem> findByWorkplaceId(Long workplaceId);
	void deleteById(Long id);
}
