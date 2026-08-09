package com.ensolution.ems.equipment.infrastructure.document;

import com.ensolution.ems.equipment.domain.EquipStatus;
import com.ensolution.ems.equipment.domain.EquipType;
import com.ensolution.ems.equipment.domain.InspectionItem;
import com.ensolution.ems.equipment.domain.spec.EquipmentSpec;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Document("equipments")
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentDocument {

	@Id
	private String id;

	@Indexed
	private Long tenantId;

	private EquipType type;

	private String managementNumber;
	private String serialNumber;
	private String modelName;
	private String equipmentName;
	private String alias;

	private BigDecimal price;
	private String manufacturer;
	private String originCountry;
	private LocalDate purchaseDate;
	private String remark;

	/**
	 * 검사 종류별 설정과 최신 상태. 도메인 {@link InspectionItem} 을 그대로 저장한다.
	 * 단순 record라 다형성 판별자가 필요 없다.
	 */
	private List<InspectionItem> inspections;

	private EquipStatus status;

	/**
	 * 장비 유형별 사양. 도메인 {@link EquipmentSpec} 을 그대로 저장하며,
	 * Spring Data MongoDB 가 {@code _class} 판별자를 기록해 구현체를 복원합니다.
	 */
	private EquipmentSpec spec;

	@CreatedDate
	private LocalDateTime createdAt;

	@LastModifiedDate
	private LocalDateTime modifiedAt;
}
