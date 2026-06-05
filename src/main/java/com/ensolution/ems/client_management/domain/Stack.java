package com.ensolution.ems.client_management.domain;

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

    public static Stack register(
        Long workplaceId,
        MeasurementField field,
        String name,
        String semsNumber,
        Grade grade,
        String businessCategory,
        String mainProduct
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
        MeasurementField field,
        String name,
        String semsNumber,
        Grade grade,
        String businessCategory,
        String mainProduct,
        String height,
        String horizontalLength,
        String verticalLength,
        Shape shape,
        Orientation orientation
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

    private static String keep(String value, String original) {
        return value == null || value.isBlank() ? original : value;
    }
}
