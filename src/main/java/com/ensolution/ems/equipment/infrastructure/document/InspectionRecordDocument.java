package com.ensolution.ems.equipment.infrastructure.document;

import com.ensolution.ems.equipment.domain.InspectionResult;
import com.ensolution.ems.equipment.domain.InspectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 검사 실시 이력. 장비 단위 최근순 조회가 유일한 조회 패턴이라 복합 인덱스를 그 형태로 둔다. */
@Document("equipment_inspection_records")
@CompoundIndex(
	name = "idx_inspection_records_tenant_equipment_inspected_at",
	def = "{'tenantId': 1, 'equipmentId': 1, 'inspectedAt': -1}"
)
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class InspectionRecordDocument {

	@Id
	private String id;

	@Indexed
	private Long tenantId;

	private String equipmentId;

	private InspectionType type;
	private LocalDate inspectedAt;
	private LocalDate validUntil;

	private String agency;
	private String certificateNumber;
	private InspectionResult result;
	private String remark;

	@CreatedDate
	private LocalDateTime createdAt;
}
