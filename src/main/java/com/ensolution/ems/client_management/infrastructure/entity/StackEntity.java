package com.ensolution.ems.client_management.infrastructure.entity;

import com.ensolution.ems.global.common.enums.Grade;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.Orientation;
import com.ensolution.ems.global.common.enums.Shape;
import com.ensolution.ems.platform.infrastructure.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Table(
	name = "stacks",
	uniqueConstraints = {
		@UniqueConstraint(
			name="uk_stacks_workplace_name_field",
			columnNames = {"workplace_id", "name", "field"}
		)
	},
	indexes = {
		@Index(
			name = "idx_stacks_tenant_id",
			columnList = "tenant_id"
		)
	}
)
@EntityListeners(AuditingEntityListener.class)
public class StackEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "stack_id")
	private Long stackId;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "tenant_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_stacks_tenants")
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private TenantEntity tenant;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "workplace_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_stacks_workplaces")
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
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

	@Column(name = "main_product")
	private String mainProduct;
	
	@Column(name = "standard_oxygen")
	private Integer standardOxygen;
	
	private Double height;

	@Column(name = "horizontal_length")
	private Double horizontalLength;

	@Column(name = "vertical_length")
	private Double verticalLength;

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
}
