package com.ensolution.ems.global.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 측정물질의 <b>측정방식 분류</b> — 이 물질이 본래 어떤 방식의 측정인가. 전 tenant를 관통하는 사실이라
 * 측정물질 가이드({@code PollutantCatalog})가 소유하고, 고객사 측정물질·측정 시점 스냅샷에 투영·복사된다.
 *
 * <p>고객사 소유 {@code MeasurementMethod}(측정방법)와 축이 다르다. 측정방법은 "이 회사가 어떻게 잡는가"
 * (채취 매체·채취 단위·채취시간)라 회사마다 갈리고 쪼개진다 — 현장측정(가스분석기)·현장측정(THC)처럼.
 * 그래도 그 항목들이 전부 현장측정이라는 사실은 변하지 않으며, 그 사실이 이 값이다. 그룹핑·정렬·통계·
 * 성적서 절 구분과 "이 항목에 고를 만한 측정방법 후보"를 좁히는 데 쓴다.
 *
 * <p>가스상 채취의 <b>매체</b>(흡수액·흡착관·테드라백·카트리지)는 여기 두지 않는다 — 같은 물질도 업체마다
 * 매체가 갈려(이황화메틸: 테드라백·카트리지) 전역 사실이 아니기 때문이다. 그 넷은 전부 {@link #GAS_SAMPLING}이다.
 *
 * <p>중금속이면서 흡수액도 하는 비소화합물처럼 두 방식에 걸치는 항목은 <b>주 방식 하나</b>를 갖는다.
 * 다중값으로 두면 그룹핑 축으로서의 값이 떨어진다. 부수 방식은 고객사 측정방법이 표현한다.
 *
 * <p>{@code schedule}의 기록지 종류({@code MeasurementCategory}: GAS·DUST·HEAVY_METAL·MERCURY)와 겹치는 것은
 * 같은 사실이기 때문이다 — 현장측정과 가스상 채취가 모두 가스상 기록지에 적힌다는 점만 다르다.
 */
@Getter
@AllArgsConstructor
public enum MeasurementMode {
	/** 직독식 장비로 현장에서 바로 읽는다. 시료가 없다. NOx·SOx·CO·THC·O₂ */
	DIRECT_READING("현장측정"),
	/** 등속흡인, 먼지 여지 */
	DUST("먼지"),
	/** 등속흡인, 중금속 여지 */
	HEAVY_METAL("중금속"),
	/** 등속흡인, 수은 흡수액 */
	MERCURY("수은"),
	/** 채취 후 실험실 분석. 매체(흡수액·흡착관·테드라백·카트리지)는 고객사 측정방법이 정한다 */
	GAS_SAMPLING("가스상 채취");

	private final String desc;
}
