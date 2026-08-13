package com.ensolution.ems.equipment.infrastructure.mapper;

import com.ensolution.ems.equipment.domain.InspectionRecord;
import com.ensolution.ems.equipment.infrastructure.document.InspectionRecordDocument;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder,
	unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface InspectionRecordDocumentMapper {

	InspectionRecordDocument toDocument(InspectionRecord record);

	InspectionRecord toDomain(InspectionRecordDocument document);

	List<InspectionRecord> toDomains(List<InspectionRecordDocument> documents);
}
