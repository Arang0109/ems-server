package com.ensolution.ems.schedule.domain.sheet;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 측정 시점 날씨 상태. */
@Getter
@RequiredArgsConstructor
public enum WeatherCondition {
	CLEAR("맑음"),
	CLOUDY("흐림"),
	RAIN("비"),
	SNOW("눈");

	private final String description;
}
