package com.ensolution.ems.storage.infrastructure.mapper;

import com.ensolution.ems.storage.application.port.in.DocumentSummary;
import com.ensolution.ems.storage.domain.Document;
import com.ensolution.ems.storage.infrastructure.entity.DocumentEntity;
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
public interface DocumentEntityMapper {

	@Mapping(target = "documentId", source = "id")
	@Mapping(target = "tenant", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	DocumentEntity toEntity(Document document);

	@Mapping(target = "id", source = "documentId")
	@Mapping(target = "tenantId", source = "tenant.tenantId")
	Document toDomain(DocumentEntity entity);

	@Mapping(target = "id", source = "documentId")
	DocumentSummary toSummary(DocumentEntity entity);

	List<DocumentSummary> toSummaries(List<DocumentEntity> entities);
}
