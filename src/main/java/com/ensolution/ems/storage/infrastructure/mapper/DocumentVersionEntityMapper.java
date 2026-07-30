package com.ensolution.ems.storage.infrastructure.mapper;

import com.ensolution.ems.storage.application.port.in.DocumentVersionSummary;
import com.ensolution.ems.storage.domain.DocumentVersion;
import com.ensolution.ems.storage.infrastructure.entity.DocumentVersionEntity;
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
public interface DocumentVersionEntityMapper {

	@Mapping(target = "documentVersionId", source = "id")
	@Mapping(target = "document", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	DocumentVersionEntity toEntity(DocumentVersion version);

	@Mapping(target = "id", source = "documentVersionId")
	@Mapping(target = "documentId", source = "document.documentId")
	DocumentVersion toDomain(DocumentVersionEntity entity);

	DocumentVersionSummary toSummary(DocumentVersionEntity entity);

	List<DocumentVersionSummary> toSummaries(List<DocumentVersionEntity> entities);
}
