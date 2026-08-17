package com.ensolution.ems.client_management.infrastructure.mapper;

import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.client_management.infrastructure.entity.PollutantEntity;
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
public interface PollutantEntityMapper {

	@Mapping(target = "pollutantId", source = "id")
	@Mapping(target = "tenant", ignore = true)
	@Mapping(target = "catalog", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	PollutantEntity toEntity(Pollutant pollutant);

	/**
	 * {@code code}·{@code field}·{@code method}·{@code phase}는 pollutants의 컬럼이 아니라
	 * 카탈로그에서 조인해 채우는 투영값이다. 측정물질 값이 필요한 모든 경로가 이 매퍼를 거치므로
	 * 여기 한 곳에서만 투영하면 화면마다 값이 달라지지 않는다.
	 */
	@Mapping(target = "id", source = "pollutantId")
	@Mapping(target = "tenantId", source = "tenant.tenantId")
	@Mapping(target = "catalogId", source = "catalog.catalogId")
	@Mapping(target = "code", source = "catalog.code")
	@Mapping(target = "field", source = "catalog.field")
	@Mapping(target = "method", source = "catalog.method")
	@Mapping(target = "phase", source = "catalog.phase")
	Pollutant toDomain(PollutantEntity entity);

	List<Pollutant> toDomainList(List<PollutantEntity> entities);
}
