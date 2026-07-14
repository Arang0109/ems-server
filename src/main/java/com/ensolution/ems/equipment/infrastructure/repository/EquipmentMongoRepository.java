package com.ensolution.ems.equipment.infrastructure.repository;

import com.ensolution.ems.equipment.domain.EquipStatus;
import com.ensolution.ems.equipment.domain.EquipType;
import com.ensolution.ems.equipment.infrastructure.document.EquipmentDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface EquipmentMongoRepository extends MongoRepository<EquipmentDocument, String> {

	Optional<EquipmentDocument> findByIdAndTenantIdAndStatusNot(String id, Long tenantId, EquipStatus status);

	List<EquipmentDocument> findByTenantIdAndStatusNot(Long tenantId, EquipStatus status);

	List<EquipmentDocument> findByTenantIdAndTypeAndStatusNot(Long tenantId, EquipType type, EquipStatus status);
}
