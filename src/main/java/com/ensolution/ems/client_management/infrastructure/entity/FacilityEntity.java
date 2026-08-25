package com.ensolution.ems.client_management.infrastructure.entity;

import com.ensolution.ems.platform.infrastructure.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Table(
	name = "facilities",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_facilities_stack_name",
			columnNames = {"stack_id", "name"}
		)
	},
	indexes = {
		@Index(
			name = "idx_facilities_tenant_id",
			columnList = "tenant_id"
		),
		// 목록 조회가 stack_id 로 걸러 sort_order 로 정렬하므로 두 컬럼을 함께 묶는다
		@Index(
			name = "idx_facilities_stack_sort",
			columnList = "stack_id, sort_order"
		)
	}
)
public class FacilityEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "facility_id")
	private Long facilityId;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "tenant_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_facilities_tenants")
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private TenantEntity tenant;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "stack_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_facilities_stacks")
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private StackEntity stack;
	
	@Column(nullable = false) private String name;
	
	@Column(name = "fuel_usage") private String fuelUsage;
	@Column(name = "product_output") private String productOutput;
	@Column(name = "incineration_amount") private String incinerationAmount;
	@Column(name = "fuel_input") private String fuelInput;
	@Column(name = "fuel_type") private String fuelType;
	@Column(name = "unit") private String unit;

	/** 측정지점 안에서의 표시 순서. nullable 이므로 정렬 시 반드시 tie-breaker 를 동반한다. */
	@Column(name = "sort_order") private Integer sortOrder;

	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
	
	@LastModifiedDate
	@Column(name = "modified_at")
	private LocalDateTime modifiedAt;
}