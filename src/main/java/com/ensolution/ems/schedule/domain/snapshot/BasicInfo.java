package com.ensolution.ems.schedule.domain.snapshot;

import com.ensolution.ems.global.common.enums.MeasurementField;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** 측정계획 기본 정보 스냅샷. 메타데이터의 사본으로, 문서 단독 조회 시 표시에 사용한다. */
public record BasicInfo(
	String referenceNumber,							// 내부 식별 코드
	
	String analyst,											// 시료분석검사자
	
	LocalDate sampledAt,								// 채취일자
	LocalDate receivedAt,								// 시료접수일자
	LocalDate analyzedAt,								// 분석완료일자
	LocalDate issuedAt,									// 성적서발행일자
	
	LocalTime samplingStartedAt,				// 채취시작시간
	LocalTime samplingEndedAt,					// 채취종료시간
	
	MeasurementField measurementField,	// 측정분야 (대기, 수질, 등)
	String schedulePurpose							// 성적서종류 (자가측정용, 기타참고용 등)
) {
	/**
	 * 메타(Schedule)에서 파생되는 필드만으로 기본 정보를 생성한다.
	 * analyst·접수/분석/발행일자·시료채취 시작/종료 시각은 성적서 발행 단계에서 채워지므로 여기서는 비워 둔다(null).
	 */
	public static BasicInfo fromMeta(String referenceNumber, LocalDate sampledAt,
	                                 MeasurementField measurementField, String schedulePurpose) {
		return new BasicInfo(
			referenceNumber,
			null,
			sampledAt,
			null,
			null,
			null,
			null,
			null,
			measurementField,
			schedulePurpose);
	}
}