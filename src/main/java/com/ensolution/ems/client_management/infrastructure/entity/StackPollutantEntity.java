package com.ensolution.ems.client_management.infrastructure.entity;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.platform.infrastructure.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@Table(
	name = "stack_pollutant",
	uniqueConstraints = {
		@UniqueConstraint( // uk_tenantId_stackId_pollutantId
			name = "uk_tenantId_stackId_pollutantId",
			columnNames = {"tenant_id", "stack_id", "pollutant_id"}
		)
	},
	indexes = {
		@Index(
			name = "idx_stackPollutant_tenantId",
			columnList = "tenant_id"
		)
	}
)
public class StackPollutantEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "stack_pollutant_id")
	private Long stackPollutantId;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "tenant_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_stackPollutant_tenants")
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private TenantEntity tenant;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "stack_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_stackPollutant_stacks")
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private StackEntity stack;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "pollutant_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_stackPollutant_pollutants")
	)
	private PollutantEntity pollutant;
	
	@Enumerated(EnumType.STRING)
	private MeasurementCycle cycle;
	
	private BigDecimal allowance;
	
	@Column(name = "oxygen_applicable")
	private boolean oxygenApplicable;
	
	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
	
	@LastModifiedDate
	@Column(name = "modified_at")
	private LocalDateTime modifiedAt;
}
