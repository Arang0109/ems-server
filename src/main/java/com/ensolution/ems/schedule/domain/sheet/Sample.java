package com.ensolution.ems.schedule.domain.sheet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

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

	/**
	 * 이 시료 한 건이 담은 측정항목(pollutantId).
	 *
	 * 기록지는 알데히드류를 {@code VOCs} 로 통칭해 시료 한 건으로 적지만 성적서는 포름알데히드·
	 * 아세트알데히드를 각각 쓴다 — 시료 1건 ↔ 항목 N건인 그 관계를 여기에 기록한다.
	 *
	 * 통칭 규칙에서 파생되는 값처럼 보이지만 파생값이 아니다. 현장에서 카트리지를 두 개 써
	 * 행을 쪼개는 순간 "포름알데히드는 A병, 아세트알데히드는 B병"은 규칙으로 복원할 수 없는
	 * 사실이 된다. 그러므로 사용자가 편집할 수 있는 입력값으로 둔다.
	 *
	 * 구 문서와 사용자가 직접 추가한 행은 null 이다.
	 */
	private List<Long> pollutantIds;
}
