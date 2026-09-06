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
	private String schedulePurpose;
	private String referenceNumber;
	private ScheduleStatus status;
	private LocalDate sampledAt;
	private LocalDate receivedAt;
	private LocalDate analyzedAt;
	private LocalDate issuedAt;
	private LocalDateTime createdAt;
	private LocalDateTime modifiedAt;

	public static Schedule register(
		Long tenantId, Long stackId, Long teamId,
		MeasurementField measurementField, String schedulePurpose, String referenceNumber,
		LocalDate sampledAt
	) {
		return Schedule.builder()
			.tenantId(tenantId)
			.stackId(stackId)
			.teamId(teamId)
			.measurementField(measurementField)
			.schedulePurpose(schedulePurpose)
			.referenceNumber(referenceNumber)
			.status(ScheduleStatus.SCHEDULED)
			.sampledAt(sampledAt)
			.build();
	}
	
	/**
	 * 측정계획의 메타정보를 수정한다. 측정일은 필수값이며, null이 전달되면 기존 측정일을 유지한다.
	 */
	public Schedule updateMetadata(LocalDate sampledAt, String schedulePurpose, String referenceNumber) {
		return this.toBuilder()
			.sampledAt(sampledAt != null ? sampledAt : this.sampledAt)
			.schedulePurpose(schedulePurpose)
			.referenceNumber(referenceNumber)
			.build();
	}
	
	/**
	 * 보고서 진행 상태의 일자를 갱신한다. <b>부분 갱신</b>이므로 전달되지 않은(null) 일자는 기존 값을 유지한다 —
	 * 현장 채취 탭과 실험·분석 탭이 이 경로를 공유해 서로 자기 것이 아닌 칸에 null을 실어 보내기 때문이다.
	 * <p>
	 * 순서 검증은 <b>병합한 뒤의 값</b>으로 한다. 요청 인자만 보고 판정하면 미전달 칸 때문에 이미 저장된
	 * 값과의 순서를 놓친다. 아직 채우지 않은 일자는 검사 대상이 아니며(진행하며 하나씩 채우는 값이다),
	 * 같은 날 접수·분석·발행이 가능하므로 경계는 ≤ 다.
	 */
	public Schedule applyReportProgress(LocalDate receivedAt, LocalDate analyzedAt, LocalDate issuedAt) {
		LocalDate mergedReceivedAt = keep(receivedAt, this.receivedAt);
		LocalDate mergedAnalyzedAt = keep(analyzedAt, this.analyzedAt);
		LocalDate mergedIssuedAt = keep(issuedAt, this.issuedAt);

		requireChronological(sampledAt, mergedReceivedAt);
		requireChronological(mergedReceivedAt, mergedAnalyzedAt);
		requireChronological(mergedAnalyzedAt, mergedIssuedAt);

		return this.toBuilder()
			.receivedAt(mergedReceivedAt)
			.analyzedAt(mergedAnalyzedAt)
			.issuedAt(mergedIssuedAt)
			.build();
	}

	private static LocalDate keep(LocalDate value, LocalDate original) {
		return value == null ? original : value;
	}

	/** 한쪽이라도 비어 있으면 아직 순서를 따질 수 없다 — 진행하며 하나씩 채우는 값이다. */
	private static void requireChronological(LocalDate before, LocalDate after) {
		if (before != null && after != null && after.isBefore(before)) {
			throw new CustomException(ErrorCode.SCHEDULE_INVALID_CHRONOLOGY);
		}
	}

	private Schedule changeStatus(ScheduleStatus next) {
		if (!this.status.canTransitionTo(next)) {
			throw new CustomException(ErrorCode.SCHEDULE_INVALID_STATUS_TRANSITION);
		}
		return this.toBuilder().status(next).build();
	}
	
	public Schedule startMeasuring() {
		return this.status == ScheduleStatus.SCHEDULED ? this.changeStatus(ScheduleStatus.MEASURING) : this;
	}
	
	public Schedule startAnalyzing() {
		return this.status == ScheduleStatus.MEASURING ? this.changeStatus(ScheduleStatus.ANALYZING) : this;
	}

	public Schedule complete() { return this.changeStatus(ScheduleStatus.REPORT_COMPLETED); }
	public Schedule cancel() { return this.changeStatus(ScheduleStatus.CANCELED); }
	public Schedule reopen() {
		if (!this.status.canReopen()) {
			throw new CustomException(ErrorCode.SCHEDULE_NOT_REOPENABLE);
		}
		return this.toBuilder().status(ScheduleStatus.SCHEDULED).build();
	}

	public void requireEditable() {
		if (!this.status.canEdit()) {
			throw new CustomException(ErrorCode.SCHEDULE_NOT_EDITABLE);
		}
	}

	public void requireSheetEditable() {
		if (!this.status.canEditSheets()) {
			throw new CustomException(ErrorCode.SCHEDULE_SHEET_NOT_EDITABLE);
		}
	}

	public void requireDeletable() {
		if (!this.status.canDelete()) {
			throw new CustomException(ErrorCode.SCHEDULE_NOT_DELETABLE);
		}
	}
}
