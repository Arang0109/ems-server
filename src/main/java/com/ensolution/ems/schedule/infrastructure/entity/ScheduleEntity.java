package com.ensolution.ems.schedule.infrastructure.entity;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.schedule.domain.ScheduleStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 측정계획 메타 엔티티. 대상(측정시설·팀)과 tenant는 다른 모듈/저장소 소유이므로
 * FK 없이 plain id 컬럼으로만 보관한다(equipment·Team 선례와 동일).
 */
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@Table(
	name = "schedules",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_schedules_stack_team_date",
			columnNames = {"stack_id", "team_id", "sampled_at"}
		)
	},
	indexes = {
		@Index(name = "idx_schedules_tenant_id", columnList = "tenant_id")
	}
)
@EntityListeners(AuditingEntityListener.class)
public class ScheduleEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "schedule_id")
	private Long scheduleId;

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "stack_id", nullable = false)
	private Long stackId;

	@Column(name = "team_id", nullable = false)
	private Long teamId;

	@Enumerated(EnumType.STRING)
	@Column(name = "measurement_field", nullable = false)
	private MeasurementField measurementField;

	@Column(name = "sampled_at", nullable = false)
	private LocalDate sampledAt;

	@Column(name = "schedule_purpose")
	private String schedulePurpose;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ScheduleStatus status;

	@Column(name = "reference_number")
	private String referenceNumber;

	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "modified_at")
	private LocalDateTime modifiedAt;
}
