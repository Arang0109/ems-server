package com.ensolution.ems.schedule.domain.sampling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 측정점 데이터. 유량(유속) 측정 입력은 항상 존재하고, 등속흡인 채취 정보({@code isokineticSampling})는
 * 입자상(먼지/중금속/수은) 시트에서만 채워진다. 입자상 채취점은 유량 측정점과 동일한 물리적 지점이므로
 * 하나의 측정점 객체가 두 정보를 함께 담아 k-factor 계산 시 같은 점의 Ts/Ps를 인덱스 조인 없이 참조한다.
 * 계산 영역(Vs·Vm·isokineticRatio 등)은 현재 파이프라인에서 재계산하지 않고 클라이언트 입력값을 보존한다(구버전과 동일).
 */
@Getter
@Jacksonized @Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class SamplingPoint {
	private BigDecimal gasTemperature; 		// 배출가스 온도 - Ts
	private BigDecimal dynamicPressure; 	// 동압 - Pv
	private BigDecimal staticPressure; 		// 정압 - Ps
	private BigDecimal gasVelocity;   		// 유속 (계산 결과)
	private BigDecimal gasDensity;

	// ===== 입자상: nullable (먼지/중금속/수은일 때만) 등속흡인계수를 계산한다. =====
	private IsokineticSamplingData isokineticSampling;

	/** 등속흡인 채취 정보. 입자상 시트에서만 존재한다. */
	@Getter
	@Jacksonized @Builder(toBuilder = true)
	@AllArgsConstructor
	@NoArgsConstructor
	public static class IsokineticSamplingData {

		private GasMeterTemperature gasTemperature;
		private GasMeterVolume gasMeterVolume;

		private BigDecimal samplingTime;             	// 채취 시간
		private BigDecimal vacuumGaugePressure;      	// 진공게이지 압력
		private BigDecimal finalImpingerTemperature; 	// 최종 임핀저 온도

		// ===== 계산 영역 =====
		private BigDecimal sampledDryGasVolume;   		// 건식가스미터 채취량 (m³) - Vm
		private BigDecimal collectedWaterVolume;  		// 채취된 물의 총량 (ml) - Vlc
		private BigDecimal kFactor;
		private BigDecimal nozzleDiameter;
		private BigDecimal orificeDifferentialPressure;
		private BigDecimal isokineticRatio; 					// 등속흡입계수

		@Getter
		@Jacksonized @Builder(toBuilder = true)
		@AllArgsConstructor
		@NoArgsConstructor
		public static class GasMeterTemperature {
			private BigDecimal inlet;
			private BigDecimal outlet;
			private BigDecimal average; // 계산 결과
		}

		@Getter
		@Jacksonized @Builder(toBuilder = true)
		@AllArgsConstructor
		@NoArgsConstructor
		public static class GasMeterVolume {
			private BigDecimal before;
			private BigDecimal after;
		}
	}
}
