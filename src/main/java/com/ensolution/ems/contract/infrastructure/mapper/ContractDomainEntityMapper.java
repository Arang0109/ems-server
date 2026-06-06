package com.ensolution.ems.contract.infrastructure.mapper;

import com.ensolution.ems.contract.application.command.ContractListItem;
import com.ensolution.ems.contract.domain.Contract;
import com.ensolution.ems.contract.infrastructure.ContractQueryRow;
import com.ensolution.ems.contract.infrastructure.entity.JpaContractEntity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder(),
	unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ContractDomainEntityMapper {
	JpaContractEntity toEntity(Contract contract);
	Contract toDomain(JpaContractEntity entity);
	
	@Mapping(target = "fieldList", expression = "java(java.util.List.of())")
	@Mapping(target = "withFieldList", ignore = true)
	ContractListItem toContractListItem(ContractQueryRow row);
	List<ContractListItem> toContractListItems(List<ContractQueryRow> rows);
}
