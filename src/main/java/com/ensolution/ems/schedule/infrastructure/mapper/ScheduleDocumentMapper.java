package com.ensolution.ems.schedule.infrastructure.mapper;

import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import com.ensolution.ems.schedule.infrastructure.document.ScheduleDocument;
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
public interface ScheduleDocumentMapper {

	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	ScheduleDocument toDocument(ScheduleSnapshot snapshot);

	// syncStatus·withSheets는 도메인 파생 메서드일 뿐 property가 아니므로 매핑 대상에서 제외한다.
	@Mapping(target = "syncStatus", ignore = true)
	@Mapping(target = "withSheets", ignore = true)
	ScheduleSnapshot toDomain(ScheduleDocument document);

	List<ScheduleSnapshot> toDomains(List<ScheduleDocument> documents);
}
