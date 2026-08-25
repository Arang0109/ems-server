package com.ensolution.ems.schedule.application.command.update;

import java.time.LocalTime;
import java.util.List;

/**
 * 성적서 항목별 채취시간 일괄 저장 파라미터.
 * 전달한 항목만 갱신하며, 전달한 항목의 null 시각은 기존 값을 비운다(미전달과 구분된다).
 */
public record SaveSamplingTimesCommand(List<Entry> items) {

	public record Entry(
		Long pollutantId,
		LocalTime samplingStartedAt,
		LocalTime samplingEndedAt
	) {}
}
