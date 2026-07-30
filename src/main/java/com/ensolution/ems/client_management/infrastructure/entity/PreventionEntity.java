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
	name = "preventions",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_preventions_stack_name",
			columnNames = {"stack_id", "name"}
		)
	},
	indexes = {
		@Index(
			name = "idx_preventions_tenant_id",
			columnList = "tenant_id"
		)
	}
)
public class PreventionEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "prevention_id")
	private Long preventionId;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "tenant_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_preventions_tenants")
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private TenantEntity tenant;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "stack_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_preventions_stacks")
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private StackEntity stack;

	@Column(nullable = false)
	private String name;
	
	@Column
	private Double capacity;
	
	@Column(name = "target_name")
	private String targetName;
	
	@Column(name = "removal_efficiency")
	private String removalEfficiency;
	
	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
	
	@LastModifiedDate
	@Column(name = "modified_at")
	private LocalDateTime modifiedAt;
}