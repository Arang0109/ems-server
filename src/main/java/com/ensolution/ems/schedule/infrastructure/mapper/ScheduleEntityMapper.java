package com.ensolution.ems.schedule.infrastructure.mapper;

import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.infrastructure.entity.ScheduleEntity;
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
public interface ScheduleEntityMapper {

	@Mapping(target = "scheduleId", source = "id")
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	ScheduleEntity toEntity(Schedule schedule);

	@Mapping(target = "id", source = "scheduleId")
	Schedule toDomain(ScheduleEntity entity);

	List<Schedule> toDomains(List<ScheduleEntity> entities);
}
