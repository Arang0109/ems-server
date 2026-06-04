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
}
