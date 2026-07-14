package com.ensolution.ems.schedule.domain;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 측정계획 메타데이터 애그리거트. 세부 스냅샷(MongoDB)과 scheduleId로 연결된다.
 * 대상(stackId·teamId)은 스냅샷 정합성을 위해 생성 이후 변경하지 않는다.
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
	private LocalDateTime measureDate;
	private String measurementType;
	private ScheduleStatus status;
	private String referenceNumber;
	private LocalDateTime createdAt;
	private LocalDateTime modifiedAt;

	public static Schedule register(
		Long tenantId,
		Long stackId,
		Long teamId,
		MeasurementField measurementField,
		LocalDateTime measureDate,
		String measurementType,
		String referenceNumber
	) {
		return Schedule.builder()
			.tenantId(tenantId)
			.stackId(stackId)
			.teamId(teamId)
			.measurementField(measurementField)
			.measureDate(measureDate)
			.measurementType(measurementType)
			.status(ScheduleStatus.SCHEDULED)
			.referenceNumber(referenceNumber)
			.build();
	}

	public Schedule update(
		MeasurementField measurementField,
		LocalDateTime measureDate,
		String measurementType,
		String referenceNumber
	) {
		return this.toBuilder()
			.measurementField(measurementField != null ? measurementField : this.measurementField)
			.measureDate(measureDate != null ? measureDate : this.measureDate)
			.measurementType(keep(measurementType, this.measurementType))
			.referenceNumber(keep(referenceNumber, this.referenceNumber))
			.build();
	}

	public Schedule changeStatus(ScheduleStatus next) {
		if (!this.status.canTransitionTo(next)) {
			throw new CustomException(ErrorCode.SCHEDULE_INVALID_STATUS_TRANSITION);
		}
		return this.toBuilder().status(next).build();
	}

	/** 편집 불가(완료·취소) 상태면 예외를 던진다. */
	public void requireEditable() {
		if (!this.status.canEdit()) {
			throw new CustomException(ErrorCode.SCHEDULE_NOT_EDITABLE);
		}
	}

	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}
}
