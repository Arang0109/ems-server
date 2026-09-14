package com.ensolution.ems.global.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 측정방법의 <b>채취 단위</b> — 그 방법으로 잡는 측정항목들이 현장 기록지의 가스상 시료 표에
 * 어떻게 적히는가. 측정방법({@code client_management.MeasurementMethod})이 소유하고,
 * 측정 시점 스냅샷({@code schedule.MeasurementMethodSnapshot})이 사본으로 갖는다.
 *
 * <ul>
 *   <li>{@link #NONE} — 가스상 시료 행을 만들지 않는다. 먼지·중금속·수은은 입자상 시트에서 등속흡인으로
 *       채취하고, 현장측정은 직독식 장비라 시료가 없다. "시료 없음"이 아니라 "가스상 표 없음"인 이유다.</li>
 *   <li>{@link #PER_ITEM} — 물질마다 병(백)이 갈려 항목별로 한 행씩 적는다. 흡수액·테드라백.</li>
 *   <li>{@link #MERGED} — <b>한 번의 채취로 그 방법의 항목 전부를 함께</b> 잡아 한 행으로 통칭한다.
 *       흡착관({@code VOCs-T})·카트리지({@code VOCs}). 통칭명은 측정방법의 {@code mergedSampleName}이다.</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
public enum SampleGrouping {
	NONE("가스상 표 없음"),
	PER_ITEM("항목별 채취"),
	MERGED("통칭 채취");

	private final String desc;
}
