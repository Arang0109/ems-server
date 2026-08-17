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
 * <p>
 * 삭제는 {@code deleted_at}을 세우는 soft delete이므로 유니크 제약에 해당 컬럼을 포함한다.
 * MySQL은 유니크 인덱스에서 NULL 중복을 허용하므로 활성 행({@code deleted_at IS NULL})끼리만
 * 유니크가 걸리고, 삭제한 계획과 같은 조건으로 다시 등록할 수 있다.
 * {@code ddl-auto: update}는 제약 변경을 반영하지 않으므로 기존 DB에는 수동 DDL이 필요하다.
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
			columnNames = {"tenant_id", "stack_id", "team_id", "sampled_at", "deleted_at"}
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

	/** soft delete 시각. null이면 활성 상태다. */
	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	/** 삭제한 사용자 id. tenant 외부 소유이므로 FK 없이 plain id로 보관한다. */
	@Column(name = "deleted_by")
	private Long deletedBy;

	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "modified_at")
	private LocalDateTime modifiedAt;
}
