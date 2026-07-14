package com.ensolution.ems.schedule.domain.sheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

/** 입자상 물질 채취 정보. {@code Cp}(피토관 계수)는 계산 파이프라인이 채우는 결과 필드. */
@Getter
@Jacksonized @Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ParticleSample {

	@JsonProperty("Cp")
	private BigDecimal Cp; // 피토관 계수 (계산 결과)
	private BigDecimal nozzleSize; // 노즐 사이즈 (cm)

	@JsonProperty("Vm")
	private BigDecimal Vm; // 건식가스미터 채취량
	private BigDecimal samplingTime;
	private LocalTime measureStartTime;
	private LocalTime measureEndTime;

	private BigDecimal kFactor;
	private BigDecimal orificeDp;
	private BigDecimal isokineticRatio;

	private LocalTime samplingStartTime;
	private LocalTime samplingEndTime;

	private String thimbleFilter;
	private String bgThimbleFilter;
}
