package com.ensolution.ems.schedule.domain.snapshot;

import com.ensolution.ems.global.common.enums.MeasurementField;

import java.time.LocalDate;
import java.time.LocalTime;

import static com.ensolution.ems.schedule.domain.snapshot.SnapshotMerge.keep;
import static com.ensolution.ems.schedule.domain.snapshot.SnapshotMerge.keepText;

/**
 * 측정계획 기본 정보 스냅샷. 메타데이터의 사본과 성적서 발행 단계 입력값을 함께 보관하며,
 * 문서 단독 조회 시 표시에 사용한다.
 * 담당자 4인(배출시설관리자·시료채취입회자·시료분석검사자·기술책임자)은 측정계획마다 달라지는 값이므로
 * 원장(의뢰기관)이 아니라 이 스냅샷이 보유한다.
 */
public record BasicInfo(
	String referenceNumber,							// 내부 식별 코드

	String facilityManager,							// 배출시설관리자
	String samplingWitness,							// 시료채취입회자(환경기술인)
	String analyst,											// 시료분석검사자
	String technicalManager,						// 기술책임자

	LocalDate sampledAt,								// 채취일자
	LocalDate receivedAt,								// 시료접수일자
	LocalDate analyzedAt,								// 분석완료일자
	LocalDate issuedAt,									// 성적서발행일자

	LocalTime samplingStartedAt,				// 채취시작시간
	LocalTime samplingEndedAt,					// 채취종료시간

	MeasurementField measurementField,	// 측정분야 (대기, 수질, 등)
	String schedulePurpose							// 측정용도 (자가측정용, 기타참고용 등)
) {
	/**
	 * 측정계획 생성 시 기본 정보를 만든다. 메타(Schedule)에서 파생되는 필드와,
	 * 사업장 원장의 배출시설관리자·시료채취입회자를 각 담당자의 초기값으로 채운다.
	 * 나머지 담당자(시료분석검사자·기술책임자)·접수/분석/발행일자·채취 시작/종료 시각은
	 * 측정·성적서 발행 단계에서 입력되므로 비워 둔다(null).
	 */
	public static BasicInfo create(String referenceNumber, LocalDate sampledAt,
	                               MeasurementField measurementField, String schedulePurpose,
	                               String facilityManager, String samplingWitness) {
		return new BasicInfo(
			referenceNumber,
			facilityManager,
			samplingWitness,
			null,
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

	/**
	 * 메타(Schedule)에서 파생되는 필드만 갱신한 새 기본 정보를 반환한다.
	 * 담당자·접수/분석/발행일자·채취 시작/종료 시각은 측정·성적서 발행 단계에서 채워지는 값이므로
	 * 메타 수정으로 유실되지 않게 기존 값을 유지한다.
	 */
	public BasicInfo applyMeta(String referenceNumber, LocalDate sampledAt,
	                           MeasurementField measurementField, String schedulePurpose) {
		return new BasicInfo(
			keepText(referenceNumber, this.referenceNumber),
			facilityManager,
			samplingWitness,
			analyst,
			technicalManager,
			keep(sampledAt, this.sampledAt),
			receivedAt,
			analyzedAt,
			issuedAt,
			samplingStartedAt,
			samplingEndedAt,
			keep(measurementField, this.measurementField),
			keepText(schedulePurpose, this.schedulePurpose));
	}

	/**
	 * 측정·성적서 발행 단계 입력값(담당자·일자·채취 시각)을 부분 갱신한 새 기본 정보를 반환한다.
	 * null(문자열은 공백 포함)인 필드는 기존 값을 유지하며, 메타에서 파생되는 필드
	 * (referenceNumber·sampledAt·measurementField·schedulePurpose)는 변경하지 않는다
	 * (해당 필드는 메타 수정 경로에서 {@link #applyMeta}로 갱신된다).
	 */
	public BasicInfo update(String facilityManager, String samplingWitness,
	                       String analyst, String technicalManager,
	                       LocalDate receivedAt, LocalDate analyzedAt, LocalDate issuedAt,
	                       LocalTime samplingStartedAt, LocalTime samplingEndedAt) {
		return new BasicInfo(
			referenceNumber,
			keepText(facilityManager, this.facilityManager),
			keepText(samplingWitness, this.samplingWitness),
			keepText(analyst, this.analyst),
			keepText(technicalManager, this.technicalManager),
			sampledAt,
			keep(receivedAt, this.receivedAt),
			keep(analyzedAt, this.analyzedAt),
			keep(issuedAt, this.issuedAt),
			keep(samplingStartedAt, this.samplingStartedAt),
			keep(samplingEndedAt, this.samplingEndedAt),
			measurementField,
			schedulePurpose);
	}
}
