package com.ensolution.ems.client_management.infrastructure.mapper;

import com.ensolution.ems.client_management.domain.MeasurementMethod;
import com.ensolution.ems.client_management.infrastructure.entity.MeasurementMethodEntity;
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
public interface MeasurementMethodEntityMapper {

	@Mapping(target = "methodId", source = "id")
	@Mapping(target = "tenant", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	MeasurementMethodEntity toEntity(MeasurementMethod method);

	@Mapping(target = "id", source = "methodId")
	@Mapping(target = "tenantId", source = "tenant.tenantId")
	MeasurementMethod toDomain(MeasurementMethodEntity entity);

	List<MeasurementMethod> toDomainList(List<MeasurementMethodEntity> entities);
}
