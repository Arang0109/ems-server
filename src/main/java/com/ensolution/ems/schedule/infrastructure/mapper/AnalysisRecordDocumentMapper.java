package com.ensolution.ems.schedule.infrastructure.mapper;

import com.ensolution.ems.schedule.domain.analysis.AnalysisRecord;
import com.ensolution.ems.schedule.infrastructure.document.AnalysisRecordDocument;
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
public interface AnalysisRecordDocumentMapper {

	// createdAt은 도메인이 왕복시킨다 — 수정 저장에서 생성 시각이 null로 지워지지 않게 하기 위해서다.
	// modifiedAt만 @LastModifiedDate가 매번 새로 채우므로 매핑에서 뺀다.
	@Mapping(target = "modifiedAt", ignore = true)
	AnalysisRecordDocument toDocument(AnalysisRecord analysisRecord);

	AnalysisRecord toDomain(AnalysisRecordDocument document);

	List<AnalysisRecord> toDomains(List<AnalysisRecordDocument> documents);
}
