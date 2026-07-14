package com.ensolution.ems.equipment.infrastructure.mapper;

import com.ensolution.ems.equipment.domain.Equipment;
import com.ensolution.ems.equipment.infrastructure.document.EquipmentDocument;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder,
	unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface EquipmentDocumentMapper {

	EquipmentDocument toDocument(Equipment equipment);

	Equipment toDomain(EquipmentDocument document);

	List<Equipment> toDomains(List<EquipmentDocument> documents);
}
