package com.ensolution.ems.contract.application.mapper;

import com.ensolution.ems.contract.application.command.ContractListItem;
import com.ensolution.ems.contract.application.port.in.ExpiringContractSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 인터모듈 매퍼: contract의 조회 VO를 타 모듈 공개용 {@code port/in} 요약 VO로 변환한다.
 */
@Mapper(componentModel = "spring")
public interface ContractSummaryMapper {

	@Mapping(target = "contractId", source = "id")
	ExpiringContractSummary toExpiringSummary(ContractListItem item);

	List<ExpiringContractSummary> toExpiringSummaries(List<ContractListItem> items);
}
