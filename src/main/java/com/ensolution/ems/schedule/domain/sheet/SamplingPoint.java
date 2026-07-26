package com.ensolution.ems.schedule.domain.sheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 측정점 데이터. 유량(유속) 측정 입력은 항상 존재하고, 등속흡인 채취 정보({@code particle})는
 * 입자상(먼지/중금속/수은) 시트에서만 채워진다. 입자상 채취점은 유량 측정점과 동일한 물리적 지점이므로
 * 하나의 측정점 객체가 두 정보를 함께 담아 k-factor 계산 시 같은 점의 Ts/Ps를 인덱스 조인 없이 참조한다.
 * 계산 영역(Vs·Vm·isokineticRatio 등)은 현재 파이프라인에서 재계산하지 않고 클라이언트 입력값을 보존한다(구버전과 동일).
 */
@Getter
@Jacksonized @Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class SamplingPoint {

	// ===== 유량: 항상 =====
	@JsonProperty("Ts")
	private BigDecimal Ts; // 배출가스 온도
	@JsonProperty("Pv")
	private BigDecimal Pv; // 동압
	@JsonProperty("Ps")
	private BigDecimal Ps; // 정압

	@JsonProperty("Vs")
	private BigDecimal Vs;   // 유속 (계산 결과)
	private BigDecimal gasDensity;

	// ===== 입자상: nullable (먼지/중금속/수은일 때만) =====
	private ParticleSampling particle;

	/** 등속흡인 채취 정보. 입자상 시트에서만 존재한다. */
	@Getter
	@Jacksonized @Builder(toBuilder = true)
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ParticleSampling {

		private EquipmentTemperature equipmentTemperature;
		private EquipmentVolume equipmentVolume;

		private BigDecimal samplingTime;             // 채취 시간
		private BigDecimal vacuumGaugePressure;      // 진공게이지 압력
		private BigDecimal finalImpingerTemperature; // 최종 임핀저 온도

		// ===== 계산 영역 =====
		@JsonProperty("Vm")
		private BigDecimal Vm;   // 건식가스미터 채취량 (m³)
		@JsonProperty("Vlc")
		private BigDecimal Vlc;  // 채취된 물의 총량 (ml)
		@JsonProperty("kFactor")
		private BigDecimal kFactor;
		private BigDecimal nozzleSize;
		private BigDecimal orificeDp;
		private BigDecimal isokineticRatio; // 등속흡입계수

		@Getter
		@Jacksonized @Builder(toBuilder = true)
		@AllArgsConstructor
		@NoArgsConstructor
		public static class EquipmentTemperature {
			private BigDecimal inTm;
			private BigDecimal outTm;
			private BigDecimal avgTm; // 계산 결과
		}

		@Getter
		@Jacksonized @Builder(toBuilder = true)
		@AllArgsConstructor
		@NoArgsConstructor
		public static class EquipmentVolume {
			private BigDecimal beforeVm;
			private BigDecimal afterVm;
		}
	}
}
