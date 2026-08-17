package com.ensolution.ems.schedule.domain;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 측정계획 메타데이터 애그리거트. 세부 스냅샷(MongoDB)과 scheduleId로 연결된다.
 * 대상(stackId·teamId)은 스냅샷 정합성을 위해 생성 이후 변경하지 않는다.
 * <p>
 * 생애주기는 서로 직교하는 두 축으로 관리한다.
 * <ul>
 *   <li><b>진행</b>({@code status}) — 측정 예정 → 측정 중 → 분석 중 → 완료, 또는 취소.
 *       업무가 실재했으나 무산된 경우가 <b>취소</b>이며 사유와 함께 이력으로 남는다.</li>
 *   <li><b>가시성</b>({@code deletedAt}) — 애초에 잘못 등록된 계획을 감추는 <b>삭제</b>.
 *       실측 데이터가 없는 '측정 예정'에서만 가능하고, 상태를 보존하므로 복구할 수 있다.</li>
 * </ul>
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Schedule {

	private Long id;
	private Long tenantId;
	private Long stackId;
	private Long teamId;
	private MeasurementField measurementField;
	private LocalDate sampledAt;
	private String schedulePurpose;
	private ScheduleStatus status;
	private String referenceNumber;
	private LocalDateTime deletedAt;
	private Long deletedBy;
	private LocalDateTime createdAt;
	private LocalDateTime modifiedAt;

	public static Schedule register(
		Long tenantId,
		Long stackId,
		Long teamId,
		MeasurementField measurementField,
		LocalDate sampledAt,
		String schedulePurpose,
		String referenceNumber
	) {
		return Schedule.builder()
			.tenantId(tenantId)
			.stackId(stackId)
			.teamId(teamId)
			.measurementField(measurementField)
			.sampledAt(sampledAt)
			.schedulePurpose(schedulePurpose)
			.status(ScheduleStatus.SCHEDULED)
			.referenceNumber(referenceNumber)
			.build();
	}

	public Schedule update(
		MeasurementField measurementField,
		LocalDate sampledAt,
		String schedulePurpose,
		String referenceNumber
	) {
		return this.toBuilder()
			.measurementField(measurementField != null ? measurementField : this.measurementField)
			.sampledAt(sampledAt != null ? sampledAt : this.sampledAt)
			.schedulePurpose(keep(schedulePurpose, this.schedulePurpose))
			.referenceNumber(keep(referenceNumber, this.referenceNumber))
			.build();
	}

	public Schedule changeStatus(ScheduleStatus next) {
		if (!this.status.canTransitionTo(next)) {
			throw new CustomException(ErrorCode.SCHEDULE_INVALID_STATUS_TRANSITION);
		}
		return this.toBuilder().status(next).build();
	}

	/**
	 * 측정 시트가 처음 입력되면 측정 착수로 간주해 측정 중으로 전진한다.
	 * 이미 진행된 상태(분석 중 등)면 그대로 둔다 — 시트 재저장이 상태를 되돌리면 안 된다.
	 * 완료·취소 건은 편집 자체가 막히므로 여기까지 도달하지 않는다.
	 */
	public Schedule startMeasuringIfScheduled() {
		return this.status == ScheduleStatus.SCHEDULED ? this.changeStatus(ScheduleStatus.MEASURING) : this;
	}

	/**
	 * 시료가 접수되면 분석 중으로 전진한다. 아직 측정 중이 아니면(예: 시트 없이 접수일부터 입력) 그대로 둔다.
	 * 상태 전이는 단계를 건너뛸 수 없으므로 측정 중을 거친 계획에만 적용된다.
	 */
	public Schedule startAnalyzingIfMeasuring() {
		return this.status == ScheduleStatus.MEASURING ? this.changeStatus(ScheduleStatus.ANALYZING) : this;
	}

	/** 측정을 마치고 완료로 확정한다. 분석 중 상태에서만 가능하다. */
	public Schedule complete() {
		return this.changeStatus(ScheduleStatus.COMPLETED);
	}

	/** 업무가 무산된 계획을 취소한다. 완료·취소된 계획은 다시 취소할 수 없다. */
	public Schedule cancel() {
		return this.changeStatus(ScheduleStatus.CANCELED);
	}

	/**
	 * 종단 상태(완료·취소)를 풀어 다시 작업 가능하게 만든다.
	 * <p>
	 * 어느 단계로 돌아갈지는 여기서 정하지 않는다. 취소는 측정 예정·측정 중·분석 중 어디에서든 걸 수 있어
	 * 되돌릴 지점이 하나로 정해지지 않는데, 그 세 단계는 모두 스냅샷에서 파생되는 상태다
	 * (완료 역시 시트·접수일이 모두 있으므로 분석 중으로 파생된다).
	 * 그래서 시작점으로만 되돌리고 실제 단계는 {@code ScheduleProgress}가 스냅샷을 보고 재도출한다.
	 *
	 * @param hasAdminPrivilege 호출자의 관리자 권한 보유 여부. 완료 재개방에만 요구한다.
	 */
	public Schedule reopen(boolean hasAdminPrivilege) {
		if (!this.status.canReopen()) {
			throw new CustomException(ErrorCode.SCHEDULE_NOT_REOPENABLE);
		}
		if (this.status.requiresAdminToReopen() && !hasAdminPrivilege) {
			throw new CustomException(ErrorCode.SCHEDULE_REOPEN_FORBIDDEN);
		}
		return this.toBuilder().status(ScheduleStatus.SCHEDULED).build();
	}

	/**
	 * 잘못 등록된 계획을 감춘다(soft delete). 상태는 그대로 보존하므로 복구할 수 있다.
	 * 측정에 착수한 계획은 취소로 처리해야 하며 여기서 예외가 발생한다.
	 */
	public Schedule delete(Long deletedBy) {
		requireDeletable();
		return this.toBuilder()
			.deletedAt(LocalDateTime.now())
			.deletedBy(deletedBy)
			.build();
	}

	/** 감춰진 계획을 되살린다. */
	public Schedule restore() {
		if (!isDeleted()) {
			throw new CustomException(ErrorCode.SCHEDULE_NOT_DELETED);
		}
		return this.toBuilder()
			.deletedAt(null)
			.deletedBy(null)
			.build();
	}

	/** 편집 불가(완료·취소·삭제) 상태면 예외를 던진다. */
	public void requireEditable() {
		requireNotDeleted();
		if (!this.status.canEdit()) {
			throw new CustomException(ErrorCode.SCHEDULE_NOT_EDITABLE);
		}
	}

	/** 삭제할 수 없는 상태(측정 착수 이후)거나 이미 삭제됐으면 예외를 던진다. */
	public void requireDeletable() {
		requireNotDeleted();
		if (!this.status.canDelete()) {
			throw new CustomException(ErrorCode.SCHEDULE_NOT_DELETABLE);
		}
	}

	private void requireNotDeleted() {
		if (isDeleted()) {
			throw new CustomException(ErrorCode.SCHEDULE_ALREADY_DELETED);
		}
	}

	public boolean isDeleted() {
		return this.deletedAt != null;
	}

	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}
}
