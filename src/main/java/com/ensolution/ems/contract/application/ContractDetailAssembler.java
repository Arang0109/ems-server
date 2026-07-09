package com.ensolution.ems.contract.application;

import com.ensolution.ems.client_management.application.port.in.ContractSummary;
import com.ensolution.ems.client_management.application.port.in.WorkplaceQueryUseCase;
import com.ensolution.ems.contract.application.command.ContractDetail;
import com.ensolution.ems.contract.domain.Contract;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Contract 도메인에 사업장·업체 요약 정보를 결합해 {@link ContractDetail}로 조립한다.
 * 조립 책임을 서비스에서 분리하여 create/update/get 각 유스케이스가 재사용한다.
 */
@Component
@RequiredArgsConstructor
public class ContractDetailAssembler {

	private final WorkplaceQueryUseCase workplaceQueryUseCase;

	public ContractDetail assemble(Contract contract) {
		ContractSummary summary = workplaceQueryUseCase.getSummaryById(contract.getWorkplaceId());
		return new ContractDetail(contract, summary.companyName(), summary.workplaceName(), summary.address());
	}
}
