package com.ensolution.ems.schedule.domain.sheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

/**
 * 배출가스 유량 집계 결과. 측정점별 원시데이터는 {@link SamplingPoint}에 있고, 여기에는 시트 단위 집계값만 담는다.
 * 유량은 측정점별이 아니라 평균값(avgPv·avgTg 등) 기준으로 단일 산출한다.
 * {@code Cp}(피토관 계수)는 유속(항상 계산)에 쓰이므로 유량 소속으로 둔다.
 */
@Getter
@Jacksonized @Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class QuantityData {

	// 계산 결과
	private BigDecimal avgTs; // 배출가스 온도 (C)
	private BigDecimal avgTg; // 배출가스 절대온도 (K)
	private BigDecimal avgPv; // 배출가스 평균 동압
	private BigDecimal avgPs; // 배출가스 평균 정압

	private BigDecimal gasDensity;    // 현장조건 배출가스 밀도
	private BigDecimal area;          // 측정시설 단면적 (m²)

	@JsonProperty("Vs")
	private BigDecimal Vs;             // 평균 배출가스 유속 (m/s)
	private BigDecimal quantity;         // 현장 습윤 유량 (m³/h)
	private BigDecimal standardQuantity; // 표준상태 건조 유량 (Sm³/h)

	@JsonProperty("Cp")
	private BigDecimal Cp; // 피토관 계수
}
