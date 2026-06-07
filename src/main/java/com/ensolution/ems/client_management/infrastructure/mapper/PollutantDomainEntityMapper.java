package com.ensolution.ems.client_management.infrastructure.mapper;

import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.client_management.infrastructure.entity.JpaPollutantEntity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder(),
	unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface PollutantDomainEntityMapper {
	JpaPollutantEntity toEntity(Pollutant pollutant);
	Pollutant toDomain(JpaPollutantEntity entity);
	List<Pollutant> toDomainList(List<JpaPollutantEntity> entities);
}
