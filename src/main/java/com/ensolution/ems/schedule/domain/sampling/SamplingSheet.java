package com.ensolution.ems.schedule.domain.sampling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * 측정 시트. 하나의 측정 카테고리(가스/중금속/먼지/수은)에 대한 측정값과 계산 결과를 담는다.
 * {@code version}은 동시 편집 충돌을 감지하기 위한 낙관적 락 토큰이다. 시트에는 식별자가 없고
 * {@code category}가 자연키이므로 충돌 판정 단위를 문서 전체가 아니라 시트로 잡는다 —
 * 두 사람이 서로 다른 시트를 나눠 입력하는 흔한 경우에는 충돌이 발생하지 않는다.
 */
@Getter
@Jacksonized
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class SamplingSheet {
	private MeasurementCategory category;					// 채취기록지 카테고리 (가스상, 먼지, 중금속, 수은)
	// 낙관적 락 토큰. 서버가 소유하며 저장할 때마다 1씩 올린다(신규 시트는 null → 0에서 시작).
	private Long version;

	private WeatherData weather;									// 기상정보 데이터
	private MoistureData moisture;								// 수분량 데이터
	private ExhaustGasData exhaustGas;						// 배출가스 데이터
	private FlowRateData flowRate;								// 유량 데이터
	
	private Integer samplingPointCount;
	private List<SamplingPoint> samplingPoints; 					// 측정점별 원시 데이터 (유량 항상 + 입자상 nullable 중첩)
	
	private ParticulateSampling particulateSampling;	// 입자상항목 데이터 (입자상 시트에만 존재)
	private List<GaseousSampling> gaseousSamplings;	// 가스항목 채취데이터

	/** 저장 성공 시 다음 버전으로 올린 시트를 반환한다(신규 시트는 0에서 시작). */
	public SamplingSheet withNextVersion() {
		return toBuilder()
			.version(version == null ? 0L : version + 1)
			.build();
	}
}
