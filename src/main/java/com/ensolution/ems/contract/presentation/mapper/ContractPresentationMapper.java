package com.ensolution.ems.contract.presentation.mapper;

import com.ensolution.ems.contract.application.command.ContractDetail;
import com.ensolution.ems.contract.application.command.ContractListItem;
import com.ensolution.ems.contract.application.command.CreateContractCommand;
import com.ensolution.ems.contract.application.command.UpdateContractCommand;
import com.ensolution.ems.contract.presentation.request.CreateContractRequest;
import com.ensolution.ems.contract.presentation.request.UpdateContractRequest;
import com.ensolution.ems.contract.presentation.response.ContractListResponse;
import com.ensolution.ems.contract.presentation.response.ContractResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder
)
public interface ContractPresentationMapper {
	CreateContractCommand toCreateCommand(CreateContractRequest request);
	UpdateContractCommand toUpdateCommand(UpdateContractRequest request);

	@Mapping(source = "contract.id", target = "id")
	@Mapping(source = "contract.workplaceId", target = "workplaceId")
	@Mapping(source = "contract.contractName", target = "contractName")
	@Mapping(source = "contract.contractDate", target = "contractDate")
	@Mapping(source = "contract.startDate", target = "startDate")
	@Mapping(source = "contract.completionDate", target = "completionDate")
	@Mapping(source = "contract.contractAccount", target = "contractAccount")
	@Mapping(source = "contract.contractAmountUnit", target = "contractAmountUnit")
	@Mapping(source = "contract.vatIncluded", target = "vatIncluded")
	@Mapping(source = "contract.contractGuaranteeAmount", target = "contractGuaranteeAmount")
	@Mapping(source = "contract.advancePaymentAmount", target = "advancePaymentAmount")
	@Mapping(source = "contract.advancePaymentDueDate", target = "advancePaymentDueDate")
	@Mapping(source = "contract.delayPenaltyRate", target = "delayPenaltyRate")
	@Mapping(source = "contract.remark", target = "remark")
	ContractResponse toResponse(ContractDetail detail);

	ContractListResponse toListResponse(ContractListItem item);
	List<ContractListResponse> toListResponses(List<ContractListItem> items);
}
