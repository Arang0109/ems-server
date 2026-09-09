package com.ensolution.ems.schedule.presentation.response.snapshot;

import com.ensolution.ems.schedule.domain.sampling.SamplingSheet;

import java.time.LocalTime;
import java.util.List;

/**
 * 그 회차의 채취 정보 응답 — 채취 시각, 현장 담당자, 채취 기록지.
 *
 * <p>{@code sheets}가 도메인 타입인 이유는 {@code SaveSheetsRequest}와 같다 — 시트는 클라이언트가
 * 읽어서 그대로 되돌려 보내는 왕복 페이로드라, 응답만 감싸면 요청과 모양이 갈라진다.
 * 특히 {@code sheets[].version}은 <b>반드시 유지된다</b> — 읽어간 시트 버전을 그대로 되돌려 보내야
 * 동시 편집 충돌을 판정할 수 있다.
 */
public record SamplingSnapshotResponse(
	LocalTime samplingStartedAt,
	LocalTime samplingEndedAt,
	String facilityManager,
	String samplingWitness,
	List<SamplingSheet> sheets
) {}
