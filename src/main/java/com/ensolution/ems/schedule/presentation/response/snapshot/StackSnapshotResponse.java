package com.ensolution.ems.schedule.presentation.response.snapshot;

import com.ensolution.ems.global.common.enums.Grade;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.Orientation;
import com.ensolution.ems.global.common.enums.Shape;

import java.util.List;

/**
 * 측정 시점 측정시설(굴뚝) 스냅샷 응답. 하위로 배출시설·방지시설을 품는다.
 * <p>
 * {@code field}는 이 시설 자체의 측정분야로, 응답 최상위의 {@code measurementField}(계획의 측정분야)와
 * 값이 같더라도 가리키는 대상이 다르다.
 */
public record StackSnapshotResponse(
	Long stackId,
	MeasurementField field,
	String name,
	String semsNumber,
	Grade grade,
	String mainProduct,
	Integer standardOxygen,
	Double height,
	Double horizontalLength,
	Double verticalLength,
	Shape shape,
	Orientation orientation,
	List<FacilitySnapshotResponse> facilities,
	List<PreventionSnapshotResponse> preventions
) {}
