package com.ensolution.ems.contract.infrastructure.mapper;

import com.ensolution.ems.contract.domain.Contract;
import com.ensolution.ems.contract.infrastructure.entity.ContractEntity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
	componentModel = "spring",
	builder = @Builder(),
	unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ContractEntityMapper {
	ContractEntity toEntity(Contract contract);
	Contract toDomain(ContractEntity entity);
}
