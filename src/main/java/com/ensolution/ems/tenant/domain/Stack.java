package com.ensolution.ems.tenant.domain;

import com.ensolution.ems.global.common.enums.Grade;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.Orientation;
import com.ensolution.ems.global.common.enums.Shape;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Stack {
	private Long id;
	private Long tenantId;
	private Long workplaceId;
	private MeasurementField field;
	private String name;
	private String semsNumber;
	private Grade grade;
	private String businessCategory;
	private String mainProduct;
	private Integer standardOxygen;
	private Double height;
	private Double horizontalLength;
	private Double verticalLength;
	private Shape shape;
	private Orientation orientation;
	private LocalDateTime createdAt;
	private LocalDateTime modifiedAt;

	public static Stack register(
		Long tenantId, Long workplaceId, MeasurementField field, String name, String semsNumber, Grade grade, String businessCategory, String mainProduct
	) {
		return Stack.builder()
			.tenantId(tenantId)
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
		MeasurementField field, String name, String semsNumber, Grade grade, String businessCategory, String mainProduct, Integer standardOxygen,
		Double height, Double horizontalLength, Double verticalLength, Shape shape, Orientation orientation
	) {
		return this.toBuilder()
			.field(field != null ? field : this.field)
			.name(keep(name, this.name))
			.semsNumber(keep(semsNumber, this.semsNumber))
			.grade(grade != null ? grade : this.grade)
			.businessCategory(keep(businessCategory, this.businessCategory))
			.mainProduct(keep(mainProduct, this.mainProduct))
			.standardOxygen(standardOxygen != null ? standardOxygen : this.standardOxygen)
			.height(height != null ? height : this.height)
			.horizontalLength(horizontalLength != null ? horizontalLength : this.height)
			.verticalLength(verticalLength != null ? verticalLength : this.height)
			.shape(shape != null ? shape : this.shape)
			.orientation(orientation != null ? orientation : this.orientation)
			.build();
	}

	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}
}
