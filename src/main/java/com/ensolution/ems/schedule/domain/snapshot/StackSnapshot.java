package com.ensolution.ems.schedule.domain.snapshot;

import com.ensolution.ems.global.common.enums.Grade;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.Orientation;
import com.ensolution.ems.global.common.enums.Shape;

import java.util.List;

/** 측정 시점 측정시설(굴뚝) 스냅샷. 하위로 배출시설·방지시설 스냅샷을 품는다. */
public record StackSnapshot(
	Long stackId,
	MeasurementField field,
	String name,
	String semsNumber,
	Grade grade,
	String businessCategory,
	String mainProduct,
	Integer standardOxygen,
	String height,
	String horizontalLength,
	String verticalLength,
	Shape shape,
	Orientation orientation,
	List<FacilitySnapshot> facilities,
	List<PreventionSnapshot> preventions
) {}
