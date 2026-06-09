package com.ensolution.ems.global.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PollutantPhase {
	PARTICLE("입자상"),
	GAS("가스상");
	
	private final String desc;
}