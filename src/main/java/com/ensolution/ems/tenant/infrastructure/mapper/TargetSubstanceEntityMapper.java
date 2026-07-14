package com.ensolution.ems.tenant.infrastructure.mapper;

import com.ensolution.ems.tenant.domain.TargetSubstance;
import com.ensolution.ems.tenant.infrastructure.entity.TargetSubstanceEntity;
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
public interface TargetSubstanceEntityMapper {
	@Mapping(target = "targetSubstanceId", source = "id")
	@Mapping(target = "tenant", ignore = true)
	@Mapping(target = "prevention", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	TargetSubstanceEntity toEntity(TargetSubstance targetSubstance);

	@Mapping(target = "id", source = "targetSubstanceId")
	@Mapping(target = "tenantId", source = "tenant.tenantId")
	@Mapping(target = "preventionId", source = "prevention.preventionId")
	TargetSubstance toDomain(TargetSubstanceEntity entity);
	
	List<TargetSubstance> toDomainList(List<TargetSubstanceEntity> entities);
}