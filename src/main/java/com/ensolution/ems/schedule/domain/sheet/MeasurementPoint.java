package com.ensolution.ems.schedule.domain.sheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 측정점 데이터. 입력 영역(Ts·Pv·Ps 등)과 계산 영역(Vs·Vm·isokineticRatio 등)으로 나뉜다.
 * 계산 영역은 현재 파이프라인에서 재계산하지 않고 클라이언트 입력값을 보존한다(구버전과 동일).
 */
@Getter
@Jacksonized @Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class MeasurementPoint {

	// ===== 입력 영역 =====
	@JsonProperty("Ts")
	private BigDecimal Ts; // 배출가스 온도
	@JsonProperty("Pv")
	private BigDecimal Pv; // 동압
	@JsonProperty("Ps")
	private BigDecimal Ps; // 정압

	private EquipmentTemperature equipmentTemperature;
	private EquipmentVolume equipmentVolume;

	private BigDecimal samplingTime;             // 채취 시간
	private BigDecimal vacuumGaugePressure;      // 진공게이지 압력
	private BigDecimal finalImpingerTemperature; // 최종 임핀저 온도

	// ===== 계산 영역 =====
	@JsonProperty("Vs")
	private BigDecimal Vs;   // 유속
	private BigDecimal gasDensity;
	@JsonProperty("Vm")
	private BigDecimal Vm;   // 건식가스미터 채취량 (m³)
	@JsonProperty("Vlc")
	private BigDecimal Vlc;  // 채취된 물의 총량 (ml)
	@JsonProperty("kFactor")
	private BigDecimal kFactor;
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
