package com.ensolution.ems.client_management.infrastructure.mapper;

import com.ensolution.ems.client_management.domain.Facility;
import com.ensolution.ems.client_management.infrastructure.entity.FacilityEntity;
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
public interface FacilityMapper {
	@Mapping(target = "stack", ignore = true)
	FacilityEntity toEntity(Facility facility);
	
	@Mapping(target = "stackId", source = "stack.id")
	Facility toDomain(FacilityEntity entity);
	
	List<Facility> toDomainList(List<FacilityEntity> entities);
}
