package com.ensolution.ems.schedule.presentation.response;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.presentation.response.snapshot.ScheduleSnapshotResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 측정계획 상세 응답. 메타데이터와 측정 시점 세부 스냅샷 트리를 함께 노출한다.
 * <p>
 * <b>최상위 필드가 진실의 원천이다.</b> 메타는 MySQL에, 스냅샷은 MongoDB에 있고 2PC를 걸 수 없어
 * 문서 쪽 사본이 어긋날 수 있으므로, 두 곳에 같은 값이 있는 항목은 최상위를 신뢰한다.
 * 그래서 {@code snapshot}은 식별자·상태 사본을 담지 않는다({@link ScheduleSnapshotResponse}).
 * <p>
 * 예외는 {@code snapshot.basicInfo}의 관리번호·채취일자·측정분야·측정용도다. 값은 최상위와 같지만
 * 그쪽은 성적서 기본정보 표의 칸이라 별개 개념으로 남긴다.
 */
public record ScheduleResponse(
	Long id,
	Long tenantId,
	Long stackId,
	Long teamId,
	MeasurementField measurementField,
	LocalDate sampledAt,
	String schedulePurpose,
	ScheduleStatus status,
	String referenceNumber,
	LocalDateTime createdAt,
	LocalDateTime modifiedAt,
	ScheduleSnapshotResponse snapshot
) {}
