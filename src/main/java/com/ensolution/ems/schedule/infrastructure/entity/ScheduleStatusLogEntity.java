package com.ensolution.ems.schedule.infrastructure.entity;

import com.ensolution.ems.schedule.domain.ScheduleStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 측정계획 상태 변경 이력 엔티티. 추가 전용(append-only)이므로 수정·삭제 경로를 두지 않는다.
 * 측정계획·사용자는 각각 다른 애그리거트/모듈 소유이므로 FK 없이 plain id 컬럼으로 보관한다
 * ({@link ScheduleEntity}와 동일한 방침).
 * <p>
 * {@code changed_at}은 도메인이 기록 시점에 값을 정하므로 auditing 리스너를 쓰지 않는다.
 */
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@Table(
	name = "schedule_status_logs",
	indexes = {
		@Index(name = "idx_schedule_status_logs_schedule", columnList = "schedule_id, changed_at"),
		@Index(name = "idx_schedule_status_logs_tenant", columnList = "tenant_id")
	}
)
public class ScheduleStatusLogEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "log_id")
	private Long logId;

	@Column(name = "schedule_id", nullable = false)
	private Long scheduleId;

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	/** 직전 상태. 최초 등록 시에는 없으므로 nullable이다. */
	@Enumerated(EnumType.STRING)
	@Column(name = "from_status")
	private ScheduleStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", nullable = false)
	private ScheduleStatus toStatus;

	/** 변경 사유. 취소·재개방은 필수이고 자동 전이는 null이다. */
	@Column(name = "reason", length = 500)
	private String reason;

	/** 스냅샷 입력에 의한 자동 전이 여부. */
	@Column(name = "automatic", nullable = false)
	private boolean automatic;

	/** 변경한 사용자 id. 자동 전이는 null이다. */
	@Column(name = "changed_by")
	private Long changedBy;

	@Column(name = "changed_at", nullable = false)
	private LocalDateTime changedAt;
}
