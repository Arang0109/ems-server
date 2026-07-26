package com.ensolution.ems.schedule.domain.sheet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 수분 정보. {@code Xw}(수분량 %)는 계산 파이프라인이 채우는 결과 필드. */
@Getter
@Jacksonized @Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class MoistureData {

	private BattleWeight weight;
	private GasMeterTemperature gasMeterTemperature;
	private DryGasVolume dryGasVolume;

	private BigDecimal suctionVelocity;
	private BigDecimal gasMeterGaugePressure;

	// 계산 결과
	private BigDecimal Pm_g;	// 수분측정용 가스미터 게이지압 (mmHg)
	private BigDecimal Tm_g;  // 가스미터 흡입 가스온도 (°C)
	private BigDecimal Vm_g;  // 흡입 건조가스량 (L)
	private BigDecimal ma;    // 흡습 수분질량 (g)
	private BigDecimal Xw; // 수분량 (%)

	/** 흡습병 무게(g). */
	@Getter
	@Jacksonized @Builder(toBuilder = true)
	@AllArgsConstructor
	@NoArgsConstructor
	public static class BattleWeight {
		private BigDecimal before;
		private BigDecimal after;
	}

	/** 가스미터 온도(°C). */
	@Getter
	@Jacksonized @Builder(toBuilder = true)
	@AllArgsConstructor
	@NoArgsConstructor
	public static class GasMeterTemperature {
		private BigDecimal in;
		private BigDecimal out;
	}

	/** 건조가스 부피(L). */
	@Getter
	@Jacksonized @Builder(toBuilder = true)
	@AllArgsConstructor
	@NoArgsConstructor
	public static class DryGasVolume {
		private BigDecimal before;
		private BigDecimal after;
	}
}
