package com.ensolution.ems.client_management.domain;

import com.ensolution.ems.global.common.enums.Grade;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.Orientation;
import com.ensolution.ems.global.common.enums.Shape;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Getter
@Table(name = "stack")
public class Stack {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workplace_id")
    @ToString.Exclude
    private Workplace workplace;
    @Column(nullable = false)
    private MeasurementField field;
    @Column(nullable = false)
    private String name;
    @Column(name = "sems_number")
    private String semsNumber;
    private Grade grade;
    @Column(name = "business_category")
    private String businessCategory;
    @Column(name = "main_product")
    private String mainProduct;

    private String height;
    @Column(name = "horizontal_length")
    private String horizontalLength;
    @Column(name = "vertical_length")
    private String verticalLength;
    private Shape shape;
    private Orientation orientation;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}
