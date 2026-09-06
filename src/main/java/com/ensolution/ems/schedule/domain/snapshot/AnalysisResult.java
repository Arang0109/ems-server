package com.ensolution.ems.schedule.domain.snapshot;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 한 측정항목의 실험실 분석 결과. {@link SamplingItemSnapshot} 안에 응집된 값 객체이며,
 * 측정계획 문서({@code schedule_documents})의 {@code items[].analysis}에 함께 저장된다.
 *
 * <p><b>판정 근거(허용기준치·산소보정 적용 여부)를 여기에 담지 않는다.</b> 항목 자신이 이미 갖고
 * 있고, 사본을 하나 더 두면 한 회차 안에서 두 값이 갈라질 수 있기 때문이다. 사본이 하나뿐이라
 * 항목이 정정될 때 분석 결과를 따라 고치는 동기화 경로 자체가 필요 없다.
 *
 * <p><b>두 화면이 필드를 나눠 소유한다.</b> 실험·분석 탭은 {@link #applyAnalysisResult}로 실험실
 * 입력 넷을, 성적서 탭은 {@link #applySamplingTime}으로 채취시각 둘을 쓴다. 두 메서드가 서로의
 * 필드를 건드리지 않는 것이, 시트와 한 문서를 쓰게 된 뒤에도 두 탭이 서로를 덮어쓰지 않는
 * 근거다. 동시 저장 규약은 {@code SnapshotWriter} 참고.
 *
 * <p>두 경로 모두 <b>전체 채택</b>이다 — 항목 표 전체를 보내는 일괄 저장이므로 빈 칸은
 * "지웠다"는 뜻이다({@code keep} 병합을 쓰지 않는다).
 *
 * <p>채취시각을 현장 채취 시트에서 파생하지 않고 성적서 탭에서 직접 받는 이유는 두 쪽의 항목
 * 단위가 다르기 때문이다. 채취기록지는 알데히드류를 {@code VOCs}로 통칭해 시료 한 건으로 적지만
 * 성적서는 포름알데히드·아세트알데히드를 각각 쓴다(시료 1건 ↔ 항목 N건). 통칭 규칙은 업체마다
 * 달라 서버가 고정할 수 없어, 자동으로 옮기면 조용히 틀린 시각이 성적서에 찍힌다.
 */
public record AnalysisResult(
	// 실험·분석 탭 소유
	BigDecimal analysisValue,      // 측정분석값
	String unit,                   // 측정단위
	String analysisMethod,         // 측정분석방법
	String analysisEquipment,      // 측정분석기기

	// 성적서 탭 소유
	LocalTime samplingStartedAt,   // 채취시작시각
	LocalTime samplingEndedAt      // 채취종료시각
) {

	/** 아직 아무것도 입력되지 않은 결과. 항목에 {@code analysis}가 없는 상태와 값이 같다. */
	public static AnalysisResult empty() {
		return new AnalysisResult(null, null, null, null, null, null);
	}

	/**
	 * 실험실 입력값만 교체한다(실험·분석 탭). 채취시각은 성적서 탭의 소유이므로 건드리지 않는다.
	 * 전달값을 그대로 채택하므로 빈 칸은 "지웠다"는 뜻이다.
	 */
	public AnalysisResult applyAnalysisResult(
		BigDecimal newAnalysisValue, String newUnit, String newAnalysisMethod, String newAnalysisEquipment
	) {
		return new AnalysisResult(
			newAnalysisValue, newUnit, newAnalysisMethod, newAnalysisEquipment,
			samplingStartedAt, samplingEndedAt);
	}

	/**
	 * 채취시각만 교체한다(성적서 탭). 실험실 입력값은 실험·분석 탭의 소유이므로 건드리지 않는다.
	 * 전달값을 그대로 채택한다. 시작·종료의 선후는 검증하지 않는다 — 자정을 넘겨 채취하는 회차가 있다.
	 */
	public AnalysisResult applySamplingTime(LocalTime newStartedAt, LocalTime newEndedAt) {
		return new AnalysisResult(
			analysisValue, unit, analysisMethod, analysisEquipment,
			newStartedAt, newEndedAt);
	}

	/** 여섯 필드가 모두 비었는지. 이행 이력에 결과를 남길지 판단하는 데 쓴다. */
	public boolean isEmpty() {
		return analysisValue == null && unit == null && analysisMethod == null
			&& analysisEquipment == null && samplingStartedAt == null && samplingEndedAt == null;
	}
}
