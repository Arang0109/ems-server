package com.ensolution.ems.client_management.infrastructure.entity;

import com.ensolution.ems.global.common.enums.Grade;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.Orientation;
import com.ensolution.ems.global.common.enums.Shape;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Table(name = "stack", uniqueConstraints = {
	@UniqueConstraint(
		name="UK_STACK_WORKPLACE_ID_FIELD_NAME",
		columnNames = {"workplace_id", "field", "name"}
	)
})
@EntityListeners(AuditingEntityListener.class)
public class StackEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workplace_id")
	@ToString.Exclude
	private WorkplaceEntity workplace;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MeasurementField field;

	@Column(nullable = false)
	private String name;

	@Column(name = "sems_number")
	private String semsNumber;

	@Enumerated(EnumType.STRING)
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

	@Enumerated(EnumType.STRING)
	private Shape shape;

	@Enumerated(EnumType.STRING)
	private Orientation orientation;

	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "modified_at")
	private LocalDateTime modifiedAt;
	
	@Builder.Default
	@OneToMany(mappedBy = "stack", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<PreventionEntity> preventions = new ArrayList<>();
	
	@Builder.Default
	@OneToMany(mappedBy = "stack", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<FacilityEntity> facilities = new ArrayList<>();
}
