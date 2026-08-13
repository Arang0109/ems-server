package com.ensolution.ems.equipment.domain;

import com.ensolution.ems.equipment.domain.spec.EquipmentSpec;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

	/**
	 * 검사 종류별 설정과 최신 상태. {@link InspectionType} 전 종류를 항상 보유하며,
	 * 검사 대상 여부는 {@link InspectionItem#enabled()} 로 표현한다.
	 * 읽을 때는 {@link #getInspections()} 가 누락된 종류를 채워 주므로 호출부가 방어할 필요가 없다.
	 */
	private List<InspectionItem> inspections;

	private EquipStatus status;

	private EquipmentSpec spec;

	private LocalDateTime createdAt;
	private LocalDateTime modifiedAt;

	public static Equipment register(
		Long tenantId, EquipType type, String managementNumber, String serialNumber, String modelName,
		String equipmentName, String alias, BigDecimal price, String manufacturer, String originCountry,
		LocalDate purchaseDate, String remark, List<InspectionItem> inspections, EquipmentSpec spec
	) {
		// 검사 항목을 주지 않으면 유형별 기본 세트를 주입한다(등록 시점 1회, 정책 변경의 소급 적용은 없다).
		List<InspectionItem> given = (inspections == null || inspections.isEmpty())
			? InspectionPolicy.defaultsFor(type)
			: inspections;

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
			.inspections(normalize(given))
			.status(EquipStatus.ACTIVE)
			.spec(spec)
			.build();
	}

	public Equipment update(
		EquipType type, String managementNumber, String serialNumber, String modelName, String equipmentName,
		String alias, BigDecimal price, String manufacturer, String originCountry, LocalDate purchaseDate,
		String remark, List<InspectionItemChange> inspectionChanges, EquipmentSpec spec
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
			.inspections(applyInspectionChanges(inspectionChanges))
			.spec(spec != null ? spec : this.spec)
			.build();
	}

	/**
	 * 검사 종류별 설정. 저장된 값에 빠진 종류가 있으면 비활성 항목으로 채워
	 * 항상 {@link InspectionType} 전 종류를 enum 선언 순서대로 반환한다.
	 * 검사 개념 도입 이전에 등록된 장비(값이 없는 문서)도 이 한 곳에서 흡수된다.
	 */
	public List<InspectionItem> getInspections() {
		return normalize(inspections);
	}

	public InspectionItem inspectionOf(InspectionType type) {
		return getInspections().stream()
			.filter(item -> item.type() == type)
			.findFirst()
			.orElse(null);
	}

	/**
	 * 알림 대상이면서 다음 예정일이 {@code dueDate} 이하인 검사 항목을 임박한 순으로 반환한다.
	 * 이미 기한을 넘긴 항목도 포함하며, 예정일을 특정할 수 없는 항목은 제외한다.
	 */
	public List<InspectionItem> notifiableItemsDueBefore(LocalDate dueDate) {
		return getInspections().stream()
			.filter(InspectionItem::notifiable)
			.filter(item -> isDueBefore(item.nextDueDate(), dueDate))
			.sorted(Comparator.comparing(InspectionItem::nextDueDate))
			.toList();
	}

	/** 검사 실시를 반영해 해당 항목의 최종 수검일(과 성적서 유효기간)을 갱신한다. */
	public Equipment recordInspection(InspectionType type, LocalDate inspectedAt, LocalDate validUntil) {
		requireInspectionEnabled(type);

		return this.toBuilder()
			.inspections(getInspections().stream()
				.map(item -> item.type() == type ? item.inspected(inspectedAt, validUntil) : item)
				.toList())
			.build();
	}

	/**
	 * 검사 대상이 아닌 종류는 실시 기록을 남길 수 없다.
	 * 포트 조회 없이 애그리거트 내부 상태만으로 판단되는 불변식이라 도메인이 소유한다.
	 */
	public void requireInspectionEnabled(InspectionType type) {
		InspectionItem item = inspectionOf(type);
		if (item == null || !item.enabled()) {
			throw new CustomException(ErrorCode.EQUIPMENT_INSPECTION_DISABLED);
		}
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

	/**
	 * 전달된 종류만 필드 단위로 병합한다. 요청에 없는 종류는 손대지 않는다.
	 * 종류가 고정 키라서 "항목 제거"라는 개념이 없고, 대상에서 빼는 것은 {@code enabled=false} 로 표현한다.
	 * 덕분에 주기만 바꾸는 흔한 수정에서 최종 수검일이 통째로 날아가는 사고가 생기지 않는다.
	 */
	private List<InspectionItem> applyInspectionChanges(List<InspectionItemChange> changes) {
		List<InspectionItem> current = getInspections();
		if (changes == null || changes.isEmpty()) {
			return current;
		}

		Map<InspectionType, InspectionItemChange> byType = changes.stream()
			.filter(change -> change != null && change.type() != null)
			.collect(Collectors.toMap(
				InspectionItemChange::type, Function.identity(), (first, second) -> second,
				() -> new EnumMap<>(InspectionType.class)
			));

		return current.stream()
			.map(item -> byType.containsKey(item.type()) ? item.merge(byType.get(item.type())) : item)
			.toList();
	}

	private static List<InspectionItem> normalize(List<InspectionItem> items) {
		Map<InspectionType, InspectionItem> byType = items == null ? Map.of() : items.stream()
			.filter(item -> item != null && item.type() != null)
			.collect(Collectors.toMap(
				InspectionItem::type, Function.identity(), (first, second) -> first,
				() -> new EnumMap<>(InspectionType.class)
			));

		return Arrays.stream(InspectionType.values())
			.map(type -> byType.getOrDefault(type, InspectionItem.disabled(type)))
			.toList();
	}

	private static boolean isDueBefore(LocalDate nextDueDate, LocalDate dueDate) {
		return nextDueDate != null && !nextDueDate.isAfter(dueDate);
	}

	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}
}
