package com.ensolution.ems.contract.infrastructure.mapper;

import com.ensolution.ems.contract.domain.Contract;
import com.ensolution.ems.contract.infrastructure.entity.JpaContractEntity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
	componentModel = "spring",
	builder = @Builder(),
	unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ContractDomainEntityMapper {
	JpaContractEntity toEntity(Contract contract);
	Contract toDomain(JpaContractEntity entity);
}
