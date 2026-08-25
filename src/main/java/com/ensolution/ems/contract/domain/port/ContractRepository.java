package com.ensolution.ems.contract.domain.port;

import com.ensolution.ems.contract.application.command.ContractListItem;
import com.ensolution.ems.contract.domain.Contract;

import java.util.List;

public interface ContractRepository {
	Contract save(Contract contract);
	/** tenant 범위 단건 조회. 미존재·타 tenant 모두 NOT_FOUND로 은닉한다. */
	Contract findById(Long id, Long tenantId);
	List<ContractListItem> findByWorkplaceId(Long workplaceId, Long tenantId);
	List<ContractListItem> findAllByTenantId(Long tenantId);
	/** tenant 범위 삭제. 삭제된 행이 없으면 NOT_FOUND. */
	void deleteById(Long id, Long tenantId);
	void deleteByWorkplaceId(Long workplaceId);
}
