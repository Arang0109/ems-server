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

	private Integer calibrationCycle; // 월 단위.
	private LocalDate lastCalibrationDate;

	private EquipStatus status;

	private EquipmentSpec spec;

	private LocalDateTime createdAt;
	private LocalDateTime modifiedAt;

	public static Equipment register(
		Long tenantId, EquipType type, String managementNumber, String serialNumber, String modelName,
		String equipmentName, String alias, BigDecimal price, String manufacturer, String originCountry,
		LocalDate purchaseDate, String remark, Integer calibrationCycle, LocalDate lastCalibrationDate,
		EquipmentSpec spec
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
			.lastCalibrationDate(lastCalibrationDate)
			.status(EquipStatus.ACTIVE)
			.spec(spec)
			.build();
	}

	public Equipment update(
		EquipType type, String managementNumber, String serialNumber, String modelName, String equipmentName,
		String alias, BigDecimal price, String manufacturer, String originCountry, LocalDate purchaseDate,
		String remark, Integer calibrationCycle, LocalDate lastCalibrationDate, EquipmentSpec spec
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
			.lastCalibrationDate(lastCalibrationDate != null ? lastCalibrationDate : this.lastCalibrationDate)
			.spec(spec != null ? spec : this.spec)
			.build();
	}

	/**
	 * 다음 교정 예정일(최종 교정일 + 교정주기). 최종 교정일이나 주기가 없으면 예정일을 특정할 수 없으므로 null을 반환한다.
	 * 교정 기한 판단의 기준이므로 외부 의존이 없는 도메인 규칙으로 여기서 소유한다.
	 */
	public LocalDate nextCalibrationDate() {
		if (lastCalibrationDate == null || calibrationCycle == null || calibrationCycle <= 0) {
			return null;
		}
		return lastCalibrationDate.plusMonths(calibrationCycle);
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
