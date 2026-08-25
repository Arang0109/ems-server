package com.ensolution.ems.schedule.domain.sheet;

import java.util.List;

/**
 * 이전 회차의 측정 시트를 다음 회차의 출발점으로 쓸 수 있게 다듬는다.
 *
 * <p>같은 측정시설이라도 회차마다 쓰는 기록지 조합이 달라지므로(먼지·수은을 함께 쓰다가 이번엔 먼지만),
 * 계획을 만들 때 미리 시트를 채워 둘 수 없다. 사용자가 기록지를 추가한 뒤 그 시트에서 직접 불러온다.
 *
 * <p><b>실측값은 지우지 않는다.</b> 사용자가 버튼을 눌러 명시적으로 불러오는 값이므로, 모르는 사이
 * 값이 들어차는 자동 채움과 위험이 다르다. 유속·온도·압력처럼 굴뚝 조건에 좌우되는 값은 다음 회차의
 * 좋은 출발점이며, 현장에서 실제로 그렇게 참고한다.
 *
 * <p>대신 <b>그 회차에만 유효한 값</b>은 지운다. 남겨 두면 고쳐야 한다는 사실조차 눈에 띄지 않아
 * 옛 값이 그대로 성적서로 나간다.
 * <ul>
 *   <li>시료번호·바탕시료번호 — 회차마다 새로 부여되는 고유값. 복사되면 분석 의뢰가 섞인다</li>
 *   <li>채취·분석기 시각 — 그날 현장에서 정해진다</li>
 *   <li>기상 조건 — 측정일의 날씨이므로 이전 값은 의미가 없다. 다만 대기압은 그날 날씨보다
 *       굴뚝이 놓인 지역의 고도에 좌우되는 값이라 남긴다</li>
 *   <li>시트 버전 — 서버 소유 토큰이다. 옛 버전을 그대로 보내면 저장이 충돌로 거부된다</li>
 * </ul>
 */
public final class SheetReuse {

	private SheetReuse() {}

	/** 다음 회차에서 쓸 수 있도록 회차 고유값을 비운 사본을 만든다. */
	public static MeasurementSheet forNextRound(MeasurementSheet source) {
		if (source == null) return null;

		return source.toBuilder()
			.version(null)
			.weather(keepPressure(source.getWeather()))
			.exhaustGas(clearTimes(source.getExhaustGas()))
			.moisture(clearTimes(source.getMoisture()))
			.particle(clearTimes(source.getParticle()))
			.samples(clearSampleIdentity(source.getSamples()))
			.build();
	}
	
	private static WeatherData keepPressure(WeatherData weather) {
		if (weather == null || weather.getPressure() == null) return null;
		return WeatherData.builder().pressure(weather.getPressure()).build();
	}

	private static ExhaustGasData clearTimes(ExhaustGasData exhaustGas) {
		if (exhaustGas == null) return null;
		return exhaustGas.toBuilder()
			.gasAnalyzerStartTime(null)
			.thcAnalyzerStartTime(null)
			.build();
	}

	private static MoistureData clearTimes(MoistureData moisture) {
		if (moisture == null) return null;
		return moisture.toBuilder()
			.samplingStartTime(null)
			.samplingEndTime(null)
			.build();
	}

	private static ParticleData clearTimes(ParticleData particle) {
		if (particle == null) return null;
		return particle.toBuilder()
			.samplingStartTime(null)
			.samplingEndTime(null)
			.build();
	}

	/**
	 * 시료 식별값과 시각만 비우고 {@code sampleName}은 남긴다 —
	 * 시료 표기 규칙은 시설마다 굳어져 있어 매번 다시 입력할 값이 아니다.
	 */
	private static List<Sample> clearSampleIdentity(List<Sample> samples) {
		if (samples == null) return null;
		return samples.stream()
			.map(sample -> sample.toBuilder()
				.sampleNumber(null)
				.blankSampleNumber(null)
				.startTime(null)
				.endTime(null)
				.build())
			.toList();
	}
}
