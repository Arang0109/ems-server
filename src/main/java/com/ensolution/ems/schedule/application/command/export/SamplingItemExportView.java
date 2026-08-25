package com.ensolution.ems.schedule.application.command.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 성적서에 실리는 측정항목 하나에 대응하는 엑셀 뷰.
 * <p>
 * <b>목록의 순서가 곧 성적서의 표기 순서다.</b> 기록부 서식은 한 장에 실을 수 있는 항목 수가
 * 정해져 있어(현대차 대기측정기록부는 4개), 템플릿이 {@code ${items[0].name}} 처럼 인덱스로
 * 칸을 지목한다. 순서는 측정계획 스냅샷에 저장된 순서를 그대로 따르며,
 * 사용자가 성적서 탭에서 재배열할 수 있다.
 * <p>
 * <b>두 출처가 한 항목에 합류한다.</b> 측정항목 정보는 측정계획 문서의 스냅샷
 * ({@code schedule_documents.items[]})에서, 실험실 입력값 4종({@code analysisValue}·{@code unit}·
 * {@code analysisMethod}·{@code analysisEquipment})과 채취시간({@code samplingStartedAt}·
 * {@code samplingEndedAt})은 실험분석정보({@code analysis_records})에서 온다.
 * 두 컬렉션은 {@code pollutantId}로 결합하며, 아직 작성되지 않은 항목은 그 값들이 모두 null이다
 * (항목 자체는 목록에서 빠지지 않는다 — 성적서 칸은 나와야 한다).
 * <p>
 * <b>채취시간은 성적서 탭에서 항목별로 직접 작성한 값이며, 현장 채취 기록지에서 파생하지 않는다.</b>
 * 기록지는 알데히드류를 {@code VOCs}로 통칭해 시료 한 건으로 적지만 성적서는 포름알데히드·
 * 아세트알데히드를 각각 쓴다(시료 1건 ↔ 항목 N건). 그 통칭 규칙이 업체마다 달라 서버가 고정할 수
 * 없으므로, 기록지의 시각을 자동으로 옮기면 조용히 틀린 값이 성적서에 찍힌다.
 * <p>
 * {@code equipment}·{@code testMethod}는 <b>측정항목 스냅샷의 시험장비·시험방법</b>이고,
 * {@code analysisEquipment}·{@code analysisMethod}는 <b>실험실의 분석장비·분석방법</b>이다.
 * 이름이 비슷하지만 서로 다른 값이므로 템플릿에서 혼동하지 않도록 한다.
 * <p>
 * {@code allowance}·{@code oxygenApplicable}은 실험분석정보에도 같은 이름의 사본이 있으나
 * <b>스냅샷 값을 쓴다</b>. 한 칸에 두 출처가 섞이면 나중에 어느 쪽이 성적서의 기준이었는지
 * 구분할 수 없게 되기 때문이다({@code MeasurementResult#ofCompletion}과 같은 판단이다).
 * <p>
 * 원장 연결키(stackPollutantId·pollutantId)는 템플릿에서 쓰이지 않으므로 노출하지 않는다.
 * jxls(JEXL)는 표준 getter로 프로퍼티를 해석하므로 record가 아니라 {@code @Getter} 클래스로 둔다.
 */
@Getter
@Builder
@AllArgsConstructor
public class SamplingItemExportView {

	// ===== 측정항목 스냅샷 출처 =====
	private final String name;             // 측정항목명(국문). 템플릿이 항목 칸에 찍는 값
	private final String nameEn;           // 측정항목명(영문)
	private final String code;             // 전역 카탈로그 키(예: NOX). 카탈로그 이전 스냅샷은 null
	private final String cycle;            // 측정주기(enum 이름)
	private final BigDecimal allowance;    // 배출허용기준. 미지정이면 null
	private final boolean oxygenApplicable;// 기준산소농도 적용 여부
	private final String equipment;        // 시험장비
	private final String testMethod;       // 시험방법

	// ===== 실험분석정보 출처 — 성적서 탭 작성분 (미작성 항목은 null) =====
	private final LocalTime samplingStartedAt; // 채취 시작시각
	private final LocalTime samplingEndedAt;   // 채취 종료시각

	// ===== 실험분석정보 출처 — 실험·분석 탭 작성분 (미분석 항목은 전부 null) =====
	private final BigDecimal analysisValue;    // 측정분석값
	private final String unit;                 // 측정단위
	private final String analysisMethod;       // 측정분석방법
	private final String analysisEquipment;    // 분석장비
}
