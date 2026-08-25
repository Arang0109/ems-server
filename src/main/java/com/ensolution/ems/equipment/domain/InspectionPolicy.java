package com.ensolution.ems.equipment.domain;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 장비 유형별 기본 검사 세트 정책.
 * <p>
 * 장비 등록 시점의 <b>초기값만</b> 결정한다. 등록 이후에는 장비별로 검사 대상 여부·주기·알림을
 * 자유롭게 바꿀 수 있으며, 이 정책을 고쳐도 <b>이미 등록된 장비에는 소급 적용하지 않는다</b>.
 * 외부 의존이 없는 도메인 규칙이므로 spring 없이 순수 static으로 둔다.
 * <p>
 * 여기서는 기본으로 <b>켤</b> 검사만 선언한다. 나머지 종류를 비활성 항목으로 채우는 정규화는
 * {@link Equipment} 가 담당하므로 정책과 정규화 규칙이 두 곳으로 갈리지 않는다.
 */
public final class InspectionPolicy {

	// ────────────────────────────────────────────────────────────────────────────
	// TODO(확인 필요): 유형별 기본 활성 검사와 기본 주기(개월).
	//   어느 장비가 어떤 검사를 받아야 하는지는 법령·기관 지침에 달린 도메인 지식이므로
	//   임의로 확정하지 않는다. 값이 정해지기 전에는 전 유형이 3종 모두 비활성으로 등록되고,
	//   사용자가 장비별로 켜서 쓴다(기능상 문제 없음).
	//
	//   예) EquipType.GAS_SAMPLER, List.of(
	//           InspectionItem.enabled(InspectionType.PRECISION_INSPECTION, 24),
	//           InspectionItem.enabled(InspectionType.CALIBRATION, 12))
	// ────────────────────────────────────────────────────────────────────────────
	private static final Map<EquipType, List<InspectionItem>> DEFAULTS = new EnumMap<>(EquipType.class);

	/** 장비 등록 시 기본으로 켜둘 검사 항목. 정책이 없는 유형은 빈 목록이다. */
	public static List<InspectionItem> defaultsFor(EquipType type) {
		if (type == null) {
			return List.of();
		}
		return DEFAULTS.getOrDefault(type, List.of());
	}

	private InspectionPolicy() {
	}
}