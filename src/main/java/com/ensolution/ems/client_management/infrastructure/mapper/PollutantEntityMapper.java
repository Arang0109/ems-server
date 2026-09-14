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
	@Mapping(target = "method", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	PollutantEntity toEntity(Pollutant pollutant);

	/**
	 * {@code code}·{@code field}·{@code phase}는 pollutants의 컬럼이 아니라
	 * 카탈로그에서 조인해 채우는 투영값이다. 측정물질 값이 필요한 모든 경로가 이 매퍼를 거치므로
	 * 여기 한 곳에서만 투영하면 화면마다 값이 달라지지 않는다.
	 * 측정방법 속성({@code methodName}·{@code sampleGrouping}·{@code mergedSampleName}·{@code methodSamplingMinutes})도
	 * 같은 방식으로 {@code method}에서 투영한다. 레거시 행은 {@code method}가 null이라 전부 null이 된다.
	 * {@code samplingMinutes}(항목 오버라이드)는 고객사 소유 컬럼이라 그대로 옮긴다.
	 */
	@Mapping(target = "id", source = "pollutantId")
	@Mapping(target = "tenantId", source = "tenant.tenantId")
	@Mapping(target = "catalogId", source = "catalog.catalogId")
	@Mapping(target = "code", source = "catalog.code")
	@Mapping(target = "field", source = "catalog.field")
	@Mapping(target = "phase", source = "catalog.phase")
	@Mapping(target = "mode", source = "catalog.mode")
	@Mapping(target = "methodId", source = "method.methodId")
	@Mapping(target = "methodName", source = "method.name")
	@Mapping(target = "sampleGrouping", source = "method.sampleGrouping")
	@Mapping(target = "mergedSampleName", source = "method.mergedSampleName")
	@Mapping(target = "methodSamplingMinutes", source = "method.samplingMinutes")
	Pollutant toDomain(PollutantEntity entity);

	List<Pollutant> toDomainList(List<PollutantEntity> entities);
}
