package com.ensolution.ems.schedule.infrastructure.mapper;

import com.ensolution.ems.schedule.domain.ScheduleStatusLog;
import com.ensolution.ems.schedule.infrastructure.entity.ScheduleStatusLogEntity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder,
	unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ScheduleStatusLogEntityMapper {

	@Mapping(target = "logId", source = "id")
	ScheduleStatusLogEntity toEntity(ScheduleStatusLog log);

	@Mapping(target = "id", source = "logId")
	ScheduleStatusLog toDomain(ScheduleStatusLogEntity entity);

	List<ScheduleStatusLog> toDomainList(List<ScheduleStatusLogEntity> entities);
}
