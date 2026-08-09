package com.ensolution.ems.equipment.infrastructure.adapter;

import com.ensolution.ems.equipment.application.port.out.InspectionRecordRepository;
import com.ensolution.ems.equipment.domain.InspectionRecord;
import com.ensolution.ems.equipment.infrastructure.document.InspectionRecordDocument;
import com.ensolution.ems.equipment.infrastructure.mapper.InspectionRecordDocumentMapper;
import com.ensolution.ems.equipment.infrastructure.repository.InspectionRecordMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class InspectionRecordRepositoryAdapter implements InspectionRecordRepository {

	private final InspectionRecordMongoRepository inspectionRecordMongoRepository;
	private final InspectionRecordDocumentMapper mapper;

	@Override
	public InspectionRecord save(InspectionRecord record) {
		InspectionRecordDocument saved = inspectionRecordMongoRepository.save(mapper.toDocument(record));
		return mapper.toDomain(saved);
	}

	@Override
	public List<InspectionRecord> findByEquipmentId(String equipmentId, Long tenantId) {
		return mapper.toDomains(
			inspectionRecordMongoRepository.findByTenantIdAndEquipmentIdOrderByInspectedAtDesc(tenantId, equipmentId)
		);
	}
}
