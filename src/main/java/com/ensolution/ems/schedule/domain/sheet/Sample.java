package com.ensolution.ems.schedule.domain.sheet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

/** 시료 채취 정보(순수 입력 데이터). */
@Getter
@Jacksonized @Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class Sample {

	private String sampleName;
	private LocalTime startTime;
	private LocalTime endTime;
	private BigDecimal suctionQuantity;
	private BigDecimal gasMeterGaugePressure;
	private BigDecimal inTemperature;
	private BigDecimal outTemperature;
	private BigDecimal beforeVolume;
	private BigDecimal afterVolume;
	private String blankSampleNumber;
	private String sampleNumber;
	private BigDecimal samplingVolume;
}
