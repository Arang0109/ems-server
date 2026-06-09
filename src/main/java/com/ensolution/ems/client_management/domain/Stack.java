package com.ensolution.ems.client_management.domain;

import com.ensolution.ems.global.common.enums.Grade;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.Orientation;
import com.ensolution.ems.global.common.enums.Shape;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Stack {
	private Long id;
	private Long workplaceId;
	private MeasurementField field;
	private String name;
	private String semsNumber;
	private Grade grade;
	private String businessCategory;
	private String mainProduct;
	private String height;
	private String horizontalLength;
	private String verticalLength;
	private Shape shape;
	private Orientation orientation;
	private LocalDateTime createdAt;
	private LocalDateTime modifiedAt;
	
	@Builder.Default private List<Prevention> preventions = new ArrayList<>();
	@Builder.Default private List<Facility> facilities = new ArrayList<>();

	public static Stack register(
		Long workplaceId, MeasurementField field, String name, String semsNumber, Grade grade, String businessCategory, String mainProduct
	) {
		return Stack.builder()
			.workplaceId(workplaceId)
			.field(field)
			.name(name)
			.semsNumber(semsNumber)
			.grade(grade)
			.businessCategory(businessCategory)
			.mainProduct(mainProduct)
			.build();
	}

	public Stack update(
		MeasurementField field, String name, String semsNumber, Grade grade, String businessCategory, String mainProduct,
		String height, String horizontalLength, String verticalLength, Shape shape, Orientation orientation
	) {
		return this.toBuilder()
			.field(field != null ? field : this.field)
			.name(keep(name, this.name))
			.semsNumber(keep(semsNumber, this.semsNumber))
			.grade(grade != null ? grade : this.grade)
			.businessCategory(keep(businessCategory, this.businessCategory))
			.mainProduct(keep(mainProduct, this.mainProduct))
			.height(keep(height, this.height))
			.horizontalLength(keep(horizontalLength, this.horizontalLength))
			.verticalLength(keep(verticalLength, this.verticalLength))
			.shape(shape != null ? shape : this.shape)
			.orientation(orientation != null ? orientation : this.orientation)
			.build();
	}
	
	public void addPrevention(Prevention prevention) {
		this.preventions.add(prevention);
	}
	
	public void removePrevention(Long preventionId) {
		Prevention prevention = findPrevention(preventionId);
		this.preventions.remove(prevention);
	}
	
	public void addPreventionTarget(Long preventionId, TargetSubstance target) {
		Prevention prevention = findPrevention(preventionId);
		prevention.addTarget(target);
	}
	
	public void removePreventionTarget(Long preventionId, Long targetId) {
		Prevention prevention = findPrevention(preventionId);
		prevention.removeTarget(targetId);
	}
	
	public void addFacility(Facility facility) {
		this.facilities.add(facility);
	}
	
	public void removeFacility(Long facilityId) {
		Facility facility = findFacility(facilityId);
		this.facilities.remove(facility);
	}
	
	private Prevention findPrevention(Long preventionId) {
		return this.preventions.stream()
			.filter((p) -> p.getId().equals(preventionId))
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
	}
	
	private Facility findFacility(Long facilityId) {
		return this.facilities.stream()
			.filter((f) -> f.getId().equals(facilityId))
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
	}

	private static String keep(String value, String original) {
			return value == null || value.isBlank() ? original : value;
	}
}
