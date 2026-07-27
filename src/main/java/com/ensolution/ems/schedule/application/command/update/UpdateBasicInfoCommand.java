package com.ensolution.ems.schedule.application.command.update;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 측정계획 문서의 기본 정보 수정 커맨드. 측정·성적서 발행 단계에서 채워지는 담당자·일자·채취 시각과
 * 측정자(멘토·멘티) 표기를 다룬다.
 * null(문자열은 공백 포함)인 필드는 기존 값을 유지하며, 메타에서 파생되는 필드
 * (측정분야·채취일자·측정용도·내부 식별 코드)는 대상이 아니다(메타 수정 경로 사용).
 */
public record UpdateBasicInfoCommand(
	String facilityManager,
	String samplingWitness,
	String analyst,
	String technicalManager,

	LocalDate receivedAt,
	LocalDate analyzedAt,
	LocalDate issuedAt,

	LocalTime samplingStartedAt,
	LocalTime samplingEndedAt,

	String mentorName,
	String menteeName
) {}
