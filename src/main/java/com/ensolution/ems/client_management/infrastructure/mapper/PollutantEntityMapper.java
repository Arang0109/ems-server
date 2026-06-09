package com.ensolution.ems.client_management.infrastructure.mapper;

import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.client_management.infrastructure.entity.PollutantEntity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder(),
	unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface PollutantEntityMapper {
	PollutantEntity toEntity(Pollutant pollutant);
	Pollutant toDomain(PollutantEntity entity);
	List<Pollutant> toDomainList(List<PollutantEntity> entities);
}
