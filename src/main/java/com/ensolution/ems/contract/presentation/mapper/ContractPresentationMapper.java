package com.ensolution.ems.contract.presentation.mapper;

import com.ensolution.ems.contract.application.command.ContractListItem;
import com.ensolution.ems.contract.application.command.CreateContractCommand;
import com.ensolution.ems.contract.application.command.UpdateContractCommand;
import com.ensolution.ems.contract.domain.Contract;
import com.ensolution.ems.contract.presentation.request.CreateContractRequest;
import com.ensolution.ems.contract.presentation.request.UpdateContractRequest;
import com.ensolution.ems.contract.presentation.response.ContractListResponse;
import com.ensolution.ems.contract.presentation.response.ContractResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder
)
public interface ContractPresentationMapper {
	CreateContractCommand toCreateCommand(CreateContractRequest request);
	UpdateContractCommand toUpdateCommand(UpdateContractRequest request);
	ContractResponse toResponse(Contract contract);
	ContractListResponse toListResponse(ContractListItem item);
	List<ContractListResponse> toListResponses(List<ContractListItem> items);
}
