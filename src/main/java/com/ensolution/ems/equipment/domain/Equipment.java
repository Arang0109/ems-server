package com.ensolution.ems.equipment.domain;

import com.ensolution.ems.equipment.domain.spec.EquipmentSpec;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Equipment {

	private String id;
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

	private Integer calibrationCycle;
	private LocalDate lastCalibrationDate;

	private EquipStatus status;

	private EquipmentSpec spec;

	private LocalDateTime createdAt;
	private LocalDateTime modifiedAt;

	public static Equipment register(
		Long tenantId, EquipType type, String managementNumber, String serialNumber, String modelName,
		String equipmentName, String alias, BigDecimal price, String manufacturer, String originCountry,
		LocalDate purchaseDate, String remark, Integer calibrationCycle, EquipmentSpec spec
	) {
		return Equipment.builder()
			.tenantId(tenantId)
			.type(type)
			.managementNumber(managementNumber)
			.serialNumber(serialNumber)
			.modelName(modelName)
			.equipmentName(equipmentName)
			.alias(alias)
			.price(price)
			.manufacturer(manufacturer)
			.originCountry(originCountry)
			.purchaseDate(purchaseDate)
			.remark(remark)
			.calibrationCycle(calibrationCycle)
			.status(EquipStatus.ACTIVE)
			.spec(spec)
			.build();
	}

	public Equipment update(
		EquipType type, String managementNumber, String serialNumber, String modelName, String equipmentName,
		String alias, BigDecimal price, String manufacturer, String originCountry, LocalDate purchaseDate,
		String remark, Integer calibrationCycle, EquipmentSpec spec
	) {
		return this.toBuilder()
			.type(type != null ? type : this.type)
			.managementNumber(keep(managementNumber, this.managementNumber))
			.serialNumber(keep(serialNumber, this.serialNumber))
			.modelName(keep(modelName, this.modelName))
			.equipmentName(keep(equipmentName, this.equipmentName))
			.alias(keep(alias, this.alias))
			.price(price != null ? price : this.price)
			.manufacturer(keep(manufacturer, this.manufacturer))
			.originCountry(keep(originCountry, this.originCountry))
			.purchaseDate(purchaseDate != null ? purchaseDate : this.purchaseDate)
			.remark(keep(remark, this.remark))
			.calibrationCycle(calibrationCycle != null ? calibrationCycle : this.calibrationCycle)
			.spec(spec != null ? spec : this.spec)
			.build();
	}

	public Equipment changeStatus(EquipStatus status) {
		return this.toBuilder()
			.status(status)
			.build();
	}

	public Equipment delete() {
		return this.toBuilder()
			.status(EquipStatus.DELETED)
			.build();
	}

	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}
}
