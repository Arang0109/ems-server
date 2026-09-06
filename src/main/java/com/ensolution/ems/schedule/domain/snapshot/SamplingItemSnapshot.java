package com.ensolution.ems.schedule.domain.snapshot;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;

import java.math.BigDecimal;

/**
 * 측정 시점 측정항목 스냅샷(시설별 측정물질 + 측정물질 마스터 결합).
 *
 * <p>이 회차에서 그 물질을 어떻게 판정하는가({@code allowance}·{@code oxygenApplicable})와
 * 실제로 얼마가 나왔는가({@code analysis})를 <b>한 원소 안에</b> 담는다. 판정 근거와 결과가
 * 갈라질 여지를 없애려는 배치이며, 그래서 둘을 잇는 색인이나 동기화 경로가 필요 없다.
 *
 * @param code     전역 측정물질 카탈로그 키(예: {@code NOX}). 카탈로그 도입 이전에 생성된 스냅샷과
 *                 고객사 자체 물질은 null이므로, 소비처는 null을 허용하고 {@code nameKr}로 폴백해야 한다
 * @param analysis 실험실 분석 결과. <b>null이면 아직 분석 전</b>이며 정상 상태다 —
 *                 {@link AnalysisResult#empty()}(입력했다가 전부 지움)와 뜻이 다르므로 구분해 다룬다
 */
public record SamplingItemSnapshot(
	Long stackPollutantId,
	Long pollutantId,
	String code,
	String nameKr,
	String nameEn,
	MeasurementField field,
	MeasurementMethod method,
	PollutantPhase phase,
	String equipment,
	String testMethod,
	MeasurementCycle cycle,
	BigDecimal allowance,
	boolean oxygenApplicable,
	AnalysisResult analysis
) {

	/**
	 * 이 회차의 측정 조건(주기·허용기준·산소보정)을 바로잡은 새 스냅샷을 반환한다.
	 * 어느 물질인지({@code pollutantId})와 원장 연결키({@code stackPollutantId}), 카탈로그 투영값은
	 * 바꾸지 않는다 — 그것이 바뀌면 다른 항목이지 정정이 아니다.
	 * 실험실 분석 결과({@code analysis})도 그대로 옮긴다. 조건 정정이 이미 나온 측정값을 지워서는 안 된다.
	 *
	 * <p><b>원장 변경을 스냅샷에 소급 반영하는 통로가 아니다.</b> 현장에서 이 회차의 기준 자체가
	 * 잘못 복사되었음을 확인했을 때 그 회차 문서를 고치는 경로이며, 호출자가 편집 가능 상태를
	 * 먼저 확인해야 한다({@code Schedule#requireEditable()}).
	 *
	 * <p>{@code allowance}·{@code oxygenApplicable}은 전달값을 그대로 채택한다.
	 * 허용기준은 "미지정"이 유효한 값이라 null을 "미전달"로 읽으면 한번 채운 뒤로는 비울 방법이
	 * 없어지고, 산소보정은 해제가 곧 false여서 유지 규칙을 둘 자리가 없다
	 * ({@code StackPollutant#update}와 같은 판단이다).
	 * {@code cycle}만 열거값이라 null을 "미전달"로 읽어 기존 값을 유지한다.
	 */
	public SamplingItemSnapshot applyCondition(
		MeasurementCycle newCycle, BigDecimal newAllowance, boolean newOxygenApplicable
	) {
		return new SamplingItemSnapshot(
			stackPollutantId, pollutantId, code, nameKr, nameEn,
			field, method, phase, equipment, testMethod,
			SnapshotMerge.keep(newCycle, cycle), newAllowance, newOxygenApplicable, analysis);
	}

	/**
	 * 실험실 분석 결과만 교체한 새 스냅샷을 반환한다. 판정 근거와 카탈로그 투영값은 그대로 둔다.
	 * 어떤 필드를 바꿀지는 {@link AnalysisResult}의 두 메서드가 정한다 —
	 * 실험·분석 탭과 성적서 탭이 서로의 입력을 덮어쓰지 않아야 하기 때문이다.
	 */
	public SamplingItemSnapshot withAnalysis(AnalysisResult newAnalysis) {
		return new SamplingItemSnapshot(
			stackPollutantId, pollutantId, code, nameKr, nameEn,
			field, method, phase, equipment, testMethod,
			cycle, allowance, oxygenApplicable, newAnalysis);
	}

	/** 아직 분석 전이면 빈 결과를, 아니면 기존 결과를 준다. 저장 경로가 null 분기를 반복하지 않도록. */
	public AnalysisResult analysisOrEmpty() {
		return analysis == null ? AnalysisResult.empty() : analysis;
	}
}
