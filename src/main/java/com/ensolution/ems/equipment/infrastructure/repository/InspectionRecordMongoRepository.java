package com.ensolution.ems.equipment.infrastructure.repository;

import com.ensolution.ems.equipment.infrastructure.document.InspectionRecordDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface InspectionRecordMongoRepository extends MongoRepository<InspectionRecordDocument, String> {

	List<InspectionRecordDocument> findByTenantIdAndEquipmentIdOrderByInspectedAtDesc(
		Long tenantId, String equipmentId
	);
}
