package com.ensolution.ems.client_management.infrastructure.mapper;

import com.ensolution.ems.client_management.domain.TargetSubstance;
import com.ensolution.ems.client_management.infrastructure.entity.TargetSubstanceEntity;
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
public interface TargetEntityMapper {
	@Mapping(target = "prevention", ignore = true)
	TargetSubstanceEntity toEntity(TargetSubstance targetSubstance);
	
	@Mapping(target = "preventionId", source = "prevention.id")
	TargetSubstance toDomain(TargetSubstanceEntity entity);
	
	List<TargetSubstance> toDomainList(List<TargetSubstanceEntity> entities);
}