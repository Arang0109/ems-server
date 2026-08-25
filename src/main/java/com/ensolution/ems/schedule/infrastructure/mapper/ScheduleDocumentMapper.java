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

	// version·createdAt은 스냅샷이 왕복시키므로 그대로 매핑한다 — 읽어온 값을 저장까지 이어야
	// 낙관적 락이 성립하고(version), 저장할 때마다 생성 시각이 null로 지워지지 않는다(createdAt).
	// modifiedAt만 @LastModifiedDate가 매번 새로 채우므로 매핑에서 뺀다.
	@Mapping(target = "modifiedAt", ignore = true)
	ScheduleDocument toDocument(ScheduleSnapshot snapshot);

	// syncStatus·withSheets·withItems는 도메인 파생 메서드일 뿐 property가 아니므로 매핑 대상에서 제외한다.
	// (인자가 하나인 fluent 메서드는 MapStruct가 타깃 property의 setter로 읽는다.)
	@Mapping(target = "syncStatus", ignore = true)
	@Mapping(target = "withSheets", ignore = true)
	@Mapping(target = "withItems", ignore = true)
	@Mapping(target = "withItemOrder", ignore = true)
	ScheduleSnapshot toDomain(ScheduleDocument document);

	List<ScheduleSnapshot> toDomains(List<ScheduleDocument> documents);
}
