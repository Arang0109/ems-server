package com.ensolution.ems.tenant.infrastructure.mapper;

import com.ensolution.ems.tenant.domain.Facility;
import com.ensolution.ems.tenant.infrastructure.entity.FacilityEntity;
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
public interface FacilityEntityMapper {
	@Mapping(target = "facilityId", source = "id")
	@Mapping(target = "tenant", ignore = true)
	@Mapping(target = "stack", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	FacilityEntity toEntity(Facility facility);

	@Mapping(target = "id", source = "facilityId")
	@Mapping(target = "tenantId", source = "tenant.tenantId")
	@Mapping(target = "stackId", source = "stack.stackId")
	Facility toDomain(FacilityEntity entity);
	
	List<Facility> toDomainList(List<FacilityEntity> entities);
}
