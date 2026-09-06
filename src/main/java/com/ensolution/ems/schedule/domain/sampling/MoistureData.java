package com.ensolution.ems.schedule.domain.sampling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

/** 수분 정보. {@code Xw}(수분량 %)는 계산 파이프라인이 채우는 결과 필드. */
@Getter
@Jacksonized @Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class MoistureData {

	private BottleWeight bottleWeight;
	private GasMeterTemperature gasMeterTemperature;
	private DryGasVolume dryGasVolume;

	private BigDecimal suctionVelocity;
	private BigDecimal gasMeterGaugePressure;
	
	private LocalTime samplingStartTime;
	private LocalTime samplingEndTime;

	// 계산 결과
	private BigDecimal gasMeterGaugePressureMmHg;		// 수분측정용 가스미터 게이지압 (mmHg) - Pm
	private BigDecimal gasMeterGaugePressureInH2O; 	// 수분측정용 가스미터 게이지압 (inchH2O)
	private BigDecimal averageGasMeterTemperature;  // 가스미터 흡입 가스온도 (°C) - Tm
	private BigDecimal sampledDryGasVolume;  				// 흡입 건조가스량 (L) - Vm
	private BigDecimal absorbedMoistureMass;    		// 흡습 수분질량 (g) - ma
	private BigDecimal moistureRatio; 							// 수분량 (%) - Xw

	/** 흡습병 무게(g). */
	@Getter
	@Jacksonized @Builder(toBuilder = true)
	@AllArgsConstructor
	@NoArgsConstructor
	public static class BottleWeight {
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
