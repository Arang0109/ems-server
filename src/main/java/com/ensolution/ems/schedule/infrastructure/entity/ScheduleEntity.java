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
 * 삭제는 행을 지우는 물리 삭제이므로 유니크 제약은 대상·팀·채취일자만으로 걸린다.
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
			columnNames = {"tenant_id", "stack_id", "team_id", "sampled_at"}
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

	/**
	 * 이 회차에 나가는 측정자(사수·부사수)의 user id. 팀 원장의 사수·부사수와 별개이며
	 * 미지정이면 null이다 — 그 경우 성적서 표기는 팀 원장의 이름으로 채워진다.
	 * 유니크 제약(대상·팀·채취일자)에는 들어가지 않는다.
	 */
	@Column(name = "mentor_id")
	private Long mentorId;

	@Column(name = "mentee_id")
	private Long menteeId;

	@Enumerated(EnumType.STRING)
	@Column(name = "measurement_field", nullable = false)
	private MeasurementField measurementField;
	
	@Column(name = "reference_number")
	private String referenceNumber;
	
	@Column(name = "schedule_purpose")
	private String schedulePurpose;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ScheduleStatus status;

	@Column(name = "sampled_at", nullable = false)
	private LocalDate sampledAt;
	
	@Column(name = "received_at")
	private LocalDate receivedAt;
	
	@Column(name = "analyzed_at")
	private LocalDate analyzedAt;
	
	@Column(name = "issued_at")
	private LocalDate issuedAt;

	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "modified_at")
	private LocalDateTime modifiedAt;
}
