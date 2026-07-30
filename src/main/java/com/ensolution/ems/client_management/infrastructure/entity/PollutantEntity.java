package com.ensolution.ems.client_management.infrastructure.entity;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;
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

import java.time.LocalDateTime;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Table(
	name = "pollutants",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_pollutant_name_tenant",
			columnNames = {"tenant_id", "name"}
		)
	},
	indexes = {
		@Index(
			name = "idx_pollutants_tenant_id",
			columnList = "tenant_id"
		)
	}
	
)
public class PollutantEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "pollutant_id")
	private Long pollutantId;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "tenant_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_pollutants_tenants")
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private TenantEntity tenant;
	
	@Enumerated(EnumType.STRING)
	private MeasurementField field;
	
	@Column(name = "name_kr", nullable = false, unique = true)
	private String nameKr;

	@Column(name = "name_en", unique = true)
	private String nameEn;
	
	@Enumerated(EnumType.STRING)
	private MeasurementMethod method;
	
	@Enumerated(EnumType.STRING)
	private PollutantPhase phase;
	
	private String equipment;
	
	@Column(name = "test_method")
	private String testMethod;
	
	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
	
	@LastModifiedDate
	@Column(name = "modified_at")
	private LocalDateTime modifiedAt;
}