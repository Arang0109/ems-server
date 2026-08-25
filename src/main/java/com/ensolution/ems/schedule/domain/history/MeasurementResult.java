package com.ensolution.ems.schedule.domain.history;

import com.ensolution.ems.schedule.domain.analysis.AnalysisRecord;
import com.ensolution.ems.schedule.domain.snapshot.SamplingItemSnapshot;

import java.math.BigDecimal;

/**
 * 한 회차에 확정된 측정항목의 결과값.
 *
 * <p>{@link MeasurementRecord} 안에 응집시켜 두는 값 객체다. 이행 사실만 있고 결과값이 아직 없는 회차
 * (현장 측정은 끝났으나 분석 결과가 들어오지 않은 항목)는 {@link #empty()}로 표현한다.
 *
 * <p>{@code allowance}를 결과에 함께 담는 이유는 초과 판정의 근거를 회차에 고정하기 위해서다.
 * 원장의 허용기준이 나중에 개정되어도 과거 판정이 뒤집히면 안 된다.
 *
 * @param exceeded 허용기준 초과 여부. 기준이나 비교값이 없으면 판정할 수 없으므로 null이다
 */
public record MeasurementResult(
	BigDecimal concentration,
	String unit,
	BigDecimal correctedConcentration,
	BigDecimal emission,
	BigDecimal allowance,
	Boolean exceeded
) {

	/** 결과값이 아직 없는 이행. */
	public static MeasurementResult empty() {
		return new MeasurementResult(null, null, null, null, null, null);
	}

	/**
	 * 측정계획이 완료될 때 확정하는 결과값. 실측 농도와 단위는 실험분석정보에서, 판정 근거인 허용기준치와
	 * 산소보정 적용 여부는 측정항목 스냅샷에서 가져온다.
	 *
	 * <p>근거를 분석 기록이 아니라 스냅샷에서 취하는 이유는 이행 기록의 다른 모든 값(주기·물질명·코드)이
	 * 스냅샷 출처이기 때문이다. 분석 결과가 없는 항목은 어차피 스냅샷을 쓸 수밖에 없으므로,
	 * 한 컬럼에 두 출처가 섞여 나중에 구분할 수 없게 되는 것을 막는다.
	 *
	 * <p>분석 결과가 아직 없으면 판정 근거만 남기고 결과값은 비운다 — 현장 측정은 이행했으므로
	 * 기록 자체는 남아야 주기 이행 현황판의 집계가 맞는다.
	 *
	 * <p><b>보정 후 농도와 배출량은 아직 채우지 않는다.</b> 산소보정계수·표준유량은 측정 시트에 있고
	 * 어느 시트를 볼지는 물질별 매핑 규칙이 정해져야 하는데, 그 규칙이 아직 확정되지 않았다.
	 * 그래서 산소보정 적용 항목은 비교값이 없어 초과 판정도 함께 보류된다.
	 */
	public static MeasurementResult ofCompletion(SamplingItemSnapshot item, AnalysisRecord analysis) {
		if (analysis == null) {
			return new MeasurementResult(null, null, null, null, item.allowance(), null);
		}
		return of(
			analysis.getAnalysisValue(),
			analysis.getUnit(),
			null,
			null,
			item.allowance(),
			item.oxygenApplicable());
	}

	/**
	 * 결과값을 확정하며 허용기준 초과 여부까지 판정한다.
	 *
	 * <p>비교 대상은 산소보정 적용 항목이면 <b>보정 후 농도</b>, 아니면 <b>실측 농도</b>다.
	 * 규제 기준이 보정 농도로 부과되는 항목을 실측값으로 견주면 초과를 놓친다.
	 */
	public static MeasurementResult of(
		BigDecimal concentration,
		String unit,
		BigDecimal correctedConcentration,
		BigDecimal emission,
		BigDecimal allowance,
		boolean oxygenApplicable
	) {
		BigDecimal compared = oxygenApplicable ? correctedConcentration : concentration;
		return new MeasurementResult(
			concentration, unit, correctedConcentration, emission, allowance, judge(compared, allowance));
	}

	/** 기준이나 비교값이 없으면 판정하지 않는다 — false로 단정하면 "기준 이내"로 잘못 읽힌다. */
	private static Boolean judge(BigDecimal compared, BigDecimal allowance) {
		if (compared == null || allowance == null) return null;
		return compared.compareTo(allowance) > 0;
	}

	public boolean hasValue() {
		return concentration != null;
	}
}
