package com.ensolution.ems.schedule.infrastructure.mapper;

import com.ensolution.ems.schedule.domain.history.MeasurementRecord;
import com.ensolution.ems.schedule.domain.history.MeasurementResult;
import com.ensolution.ems.schedule.infrastructure.entity.MeasurementRecordEntity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 결과값은 도메인에서 {@link MeasurementResult} 값 객체로 묶여 있고 테이블에서는 평탄한 컬럼이므로,
 * 양방향 모두 중첩 경로를 명시해 매핑한다.
 */
@Mapper(
	componentModel = "spring",
	builder = @Builder,
	unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface MeasurementRecordEntityMapper {

	@Mapping(target = "recordId", source = "id")
	@Mapping(target = "concentration", source = "result.concentration")
	@Mapping(target = "unit", source = "result.unit")
	@Mapping(target = "correctedConcentration", source = "result.correctedConcentration")
	@Mapping(target = "emission", source = "result.emission")
	@Mapping(target = "allowance", source = "result.allowance")
	@Mapping(target = "exceeded", source = "result.exceeded")
	MeasurementRecordEntity toEntity(MeasurementRecord record);

	@Mapping(target = "id", source = "entity.recordId")
	@Mapping(target = "result", source = "entity")
	MeasurementRecord toDomain(MeasurementRecordEntity entity);

	MeasurementResult toResult(MeasurementRecordEntity entity);

	List<MeasurementRecord> toDomainList(List<MeasurementRecordEntity> entities);

	List<MeasurementRecordEntity> toEntities(List<MeasurementRecord> records);
}
