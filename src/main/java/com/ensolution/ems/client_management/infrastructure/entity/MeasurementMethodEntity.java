package com.ensolution.ems.client_management.infrastructure.entity;

import com.ensolution.ems.global.common.enums.SampleGrouping;
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

import java.time.LocalDateTime;

/**
 * 고객사 측정방법. 채취 단위·통칭 시료명·표준 채취시간을 소유하며 {@code pollutants.method_id}가 참조한다.
 * 제약 이름은 {@code docs/migration/2026-09-14-measurement-methods.sql}의 DDL과 같아야 한다 —
 * 다르면 {@code ddl-auto: update}가 같은 제약을 한 번 더 만든다.
 */
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@Table(
	name = "measurement_methods",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_measurement_methods_tenant_name",
			columnNames = {"tenant_id", "name"}
		)
	},
	indexes = {
		@Index(
			name = "idx_measurement_methods_tenant_id",
			columnList = "tenant_id"
		)
	}
)
public class MeasurementMethodEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "method_id")
	private Long methodId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "tenant_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_measurement_methods_tenants")
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private TenantEntity tenant;

	@Column(name = "name", nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "sample_grouping", nullable = false)
	private SampleGrouping sampleGrouping;

	/** MERGED일 때만 값이 있다. 불변식은 도메인이 지킨다. */
	@Column(name = "merged_sample_name")
	private String mergedSampleName;

	/** 표준(계획) 채취시간, 분. */
	@Column(name = "sampling_minutes")
	private Integer samplingMinutes;

	@Column(name = "sort_order")
	private Integer sortOrder;

	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "modified_at")
	private LocalDateTime modifiedAt;
}
