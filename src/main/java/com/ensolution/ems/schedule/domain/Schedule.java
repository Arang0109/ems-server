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
 * 생애주기는 {@code status} 하나로 관리한다 — 측정 예정 → 측정 중 → 분석값 입력 중 →
 * 성적서 작성 완료, 또는 취소. 업무가 실재했으나 무산된 경우가 <b>취소</b>이고,
 * 애초에 잘못 등록된 계획은 <b>삭제</b>로 지운다(물리 삭제, 복구 불가).
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

	private Schedule changeStatus(ScheduleStatus next) {
		if (!this.status.canTransitionTo(next)) {
			throw new CustomException(ErrorCode.SCHEDULE_INVALID_STATUS_TRANSITION);
		}
		return this.toBuilder().status(next).build();
	}
	
	public Schedule startMeasuringIfScheduled() {
		return this.status == ScheduleStatus.SCHEDULED ? this.changeStatus(ScheduleStatus.MEASURING) : this;
	}
	
	public Schedule startAnalyzingIfMeasuring() {
		return this.status == ScheduleStatus.MEASURING ? this.changeStatus(ScheduleStatus.ANALYZING) : this;
	}

	public Schedule complete() {
		return this.changeStatus(ScheduleStatus.REPORT_COMPLETED);
	}

	public Schedule cancel() {
		return this.changeStatus(ScheduleStatus.CANCELED);
	}

	public Schedule reopen() {
		if (!this.status.canReopen()) {
			throw new CustomException(ErrorCode.SCHEDULE_NOT_REOPENABLE);
		}
		return this.toBuilder().status(ScheduleStatus.SCHEDULED).build();
	}

	/** 편집 불가(완료·취소) 상태면 예외를 던진다. */
	public void requireEditable() {
		if (!this.status.canEdit()) {
			throw new CustomException(ErrorCode.SCHEDULE_NOT_EDITABLE);
		}
	}

	/**
	 * 삭제할 수 없는 상태(진행 중)면 예외를 던진다.
	 * 삭제는 되돌릴 수 없으므로 실측 데이터가 없는 '측정 예정'과 '취소'에서만 허용한다
	 * ({@link ScheduleStatus#canDelete()}).
	 */
	public void requireDeletable() {
		if (!this.status.canDelete()) {
			throw new CustomException(ErrorCode.SCHEDULE_NOT_DELETABLE);
		}
	}

	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}
}
