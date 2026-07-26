package com.ensolution.ems.global.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Grade {
	TYPE_1("1종"),
	TYPE_2("2종"),
	TYPE_3("3종"),
	TYPE_4("4종"),
	TYPE_5("5종");
	
	private final String desc;
}