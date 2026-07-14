package com.ensolution.ems.contract.infrastructure.mapper;

import com.ensolution.ems.contract.domain.Contract;
import com.ensolution.ems.contract.infrastructure.entity.ContractEntity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
	componentModel = "spring",
	builder = @Builder(),
	unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ContractEntityMapper {
	@Mapping(target = "contractId", source = "id")
	ContractEntity toEntity(Contract contract);
	
	@Mapping(target = "id", source = "contractId")
	Contract toDomain(ContractEntity entity);
}
