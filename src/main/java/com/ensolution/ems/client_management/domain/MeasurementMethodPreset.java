package com.ensolution.ems.client_management.domain;

import com.ensolution.ems.global.common.enums.SampleGrouping;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 측정방법 기본 8종. 전역 enum이던 시절의 값을 그대로 옮긴 것이다.
 *
 * <p>두 곳에서 쓴다 — (1) {@code MeasurementMethodService.ensureDefaults}가 tenant에 이름 기준으로 채워 넣고,
 * (2) {@code docs/migration/2026-09-14-measurement-methods.sql}이 기존 {@code pollutants.method} enum 값을
 * 이 표의 이름으로 매핑해 백필한다. <b>이 표와 마이그레이션의 CASE 매핑은 반드시 같아야 한다.</b>
 *
 * <p>표준 채취시간은 기본값이 없다. 업체마다 다르므로 tenant가 채운다.
 */
@Getter
@AllArgsConstructor
public enum MeasurementMethodPreset {
	DUST("먼지", SampleGrouping.NONE, null, 10),
	HEAVY_METAL("중금속", SampleGrouping.NONE, null, 20),
	MERCURY("수은", SampleGrouping.NONE, null, 30),
	FIELD_MEASUREMENT("현장측정", SampleGrouping.NONE, null, 40),
	ABSORPTION_SOLUTION("흡수액", SampleGrouping.PER_ITEM, null, 50),
	ADSORPTION_TUBE("흡착관", SampleGrouping.MERGED, "VOCs-T", 60),
	TEDLAR_BAG("테드라백", SampleGrouping.PER_ITEM, null, 70),
	CARTRIDGE("카트리지", SampleGrouping.MERGED, "VOCs", 80);

	private final String methodName;
	private final SampleGrouping sampleGrouping;
	private final String mergedSampleName;
	private final int sortOrder;

	public MeasurementMethod toDomain(Long tenantId) {
		return MeasurementMethod.register(tenantId, methodName, sampleGrouping, mergedSampleName, null, sortOrder);
	}
}
