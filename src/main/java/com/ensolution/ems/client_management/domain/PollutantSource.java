package com.ensolution.ems.client_management.domain;

/**
 * 측정물질 선택 목록 항목의 상태. 프론트가 "아직 채택하지 않은 가이드 항목"과 "이미 채택해 쓰고 있는 물질"을
 * 구분하기 위한 값이다.
 *
 * <p>고객사는 가이드({@link PollutantCatalog})에 없는 물질을 만들 수 없으므로 두 상태만 존재한다.
 */
public enum PollutantSource {
	/** 가이드에는 있으나 이 tenant가 아직 채택하지 않음. pollutantId가 null이다. */
	CATALOG,
	/** 이 tenant가 채택해 보유 중인 물질. 표기명·장비·시험방법은 고객사 소유값이다. */
	REGISTERED
}
