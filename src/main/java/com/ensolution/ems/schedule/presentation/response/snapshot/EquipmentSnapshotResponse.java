package com.ensolution.ems.schedule.presentation.response.snapshot;

import com.ensolution.ems.equipment.domain.EquipType;
import com.ensolution.ems.equipment.domain.InspectionItem;
import com.ensolution.ems.equipment.domain.spec.EquipmentSpec;

import java.util.List;

/**
 * 측정 시점 장비 스냅샷 응답.
 * <p>
 * {@code inspections}·{@code spec}은 equipment 도메인 타입을 그대로 노출한다. equipment 모듈이
 * 이 타입들을 <b>공유 커널</b>로 공개하고 있고(해당 모듈 {@code .claude/CLAUDE.md}), 형제 응답인
 * {@code EquipmentResponse}도 동일하게 노출한다. sealed 계층인 {@link EquipmentSpec}을 여기서만
 * 다시 감싸면 다형 구조를 중복 정의하면서 형제 모듈과 응답 모양이 어긋난다.
 */
public record EquipmentSnapshotResponse(
	String equipmentId,
	EquipType type,
	String managementNumber,
	String serialNumber,
	String modelName,
	String equipmentName,
	String alias,
	String manufacturer,
	List<InspectionItem> inspections,
	EquipmentSpec spec
) {}
