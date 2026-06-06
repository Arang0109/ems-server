package com.ensolution.ems.contract.infrastructure.mapper;

import com.ensolution.ems.contract.application.command.ContractListItem;
import com.ensolution.ems.contract.domain.Contract;
import com.ensolution.ems.contract.infrastructure.entity.JpaContractEntity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
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
	ContractListItem toContractListItem(JpaContractEntity entity);
	List<ContractListItem> toContractListItems(List<JpaContractEntity> entities);
}
