package com.ensolution.ems.tenant.infrastructure.entity;

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
	name = "target_substances",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_target_substances_prevention_name",
			columnNames = {"prevention_id", "name"}
		)
	},
	indexes = {
		@Index(
			name = "idx_target_substances_tenant_id",
			columnList = "tenant_id"
		)
	}
)
public class TargetSubstanceEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "target_substance_id")
	private Long targetSubstanceId;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "tenant_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_target_substances_tenants")
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private TenantEntity tenant;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "prevention_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_target_substances_preventions")
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private PreventionEntity prevention;
	
	@Column(nullable = false)
	private String name;
	
	@Column(name = "removal_efficiency")
	private Double removalEfficiency;
	
	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
	
	@LastModifiedDate
	@Column(name = "modified_at")
	private LocalDateTime modifiedAt;
}