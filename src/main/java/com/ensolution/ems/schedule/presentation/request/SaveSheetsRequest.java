package com.ensolution.ems.schedule.presentation.request;

import com.ensolution.ems.schedule.domain.sampling.SamplingSheet;
import com.ensolution.ems.schedule.domain.sampling.SheetRef;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

/**
 * 측정 시트 저장 요청. 시트 구조가 방대하고 계산 도메인과 1:1이므로 도메인 시트를 그대로 입력받는다
 * (presentation → domain 참조는 의존 방향에 부합). 저장 시 서버가 계산 파이프라인을 실행한다.
 * <p>
 * 각 시트는 읽어간 시점의 {@code version}을 지녀야 하며, 서버는 이 값으로 동시 편집 충돌을 판정한다.
 * 요청에 담기지 않은 카테고리의 시트는 서버 값이 그대로 유지되므로, 시트 삭제는
 * {@code deletedSheets}로 명시해야 한다 — 그래야 내가 화면을 연 뒤 다른 사용자가 추가한
 * 시트가 내 저장으로 조용히 사라지지 않는다.
 * <p>
 * 채취 시각과 현장 담당자를 함께 싣는다. 같은 채취 스냅샷 노드에 살고 현장 채취 탭이 함께 소유하므로,
 * 나눠 보내면 저장 한 번이 여러 왕복이 되고 중간에 실패하면 화면 상태가 갈라진다.
 * 이 넷은 전달되지 않은(공백 포함) 값이 기존 값을 유지하는 <b>부분 갱신</b>이다.
 */
public record SaveSheetsRequest(
	@Schema(description = "채취 시작시각. 입력되면 상태가 측정 중으로 전진합니다.", example = "09:30:00")
	LocalTime samplingStartedAt,

	@Schema(description = "채취 종료시각", example = "11:00:00")
	LocalTime samplingEndedAt,

	@Schema(description = "배출시설관리자")
	String facilityManager,

	@Schema(description = "시료채취입회자(환경기술인)")
	String samplingWitness,

	@NotNull(message = "측정 시트는 필수 값입니다.")
	List<SamplingSheet> sheets,

	List<SheetRef> deletedSheets
) {}
