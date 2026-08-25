package com.ensolution.ems.equipment.domain;

import java.time.LocalDate;

/**
 * 장비 한 대의 검사 종류 하나에 대한 설정과 최신 상태.
 * <p>
 * 장비는 {@link InspectionType} 전 종류의 항목을 항상 보유하며, 검사 대상 여부는 목록에서
 * 넣고 빼는 대신 {@code enabled} 로 표현한다. 종류가 고정 키라서 중복이 생길 수 없고,
 * 수정 시 항목이 사라지면서 수검 이력이 함께 날아가는 사고도 구조적으로 막힌다.
 *
 * @param enabled             이 장비가 이 검사를 받는 대상인지
 * @param cycleMonths         검사 주기(개월)
 * @param lastInspectedAt     최종 수검일. 검사 실시 기록({@link InspectionRecord})이 갱신한다
 * @param nextDueDateOverride 성적서에 유효기간이 직접 적힌 경우의 예정일. 있으면 주기 계산보다 우선한다
 * @param notificationEnabled 예정일 임박 알림 수신 여부. {@code enabled} 가 true일 때만 의미가 있다
 */
public record InspectionItem(
	InspectionType type,
	boolean enabled,
	Integer cycleMonths,
	LocalDate lastInspectedAt,
	LocalDate nextDueDateOverride,
	boolean notificationEnabled
) {

	/** 검사 대상이 아닌 항목. 장비 등록·정규화 시 빈 자리를 채운다. */
	public static InspectionItem disabled(InspectionType type) {
		return new InspectionItem(type, false, null, null, null, true);
	}

	/** 검사 대상 항목. 알림은 기본으로 켠다. */
	public static InspectionItem enabled(InspectionType type, Integer cycleMonths) {
		return new InspectionItem(type, true, cycleMonths, null, null, true);
	}

	/**
	 * 다음 검사 예정일. 검사 대상이 아니거나 예정일을 특정할 수 없으면 null이다.
	 * 성적서 유효기간이 지정돼 있으면 그 값을, 아니면 최종 수검일 + 주기를 쓴다.
	 */
	public LocalDate nextDueDate() {
		if (!enabled) {
			return null;
		}
		if (nextDueDateOverride != null) {
			return nextDueDateOverride;
		}
		if (lastInspectedAt == null || cycleMonths == null || cycleMonths <= 0) {
			return null;
		}
		return lastInspectedAt.plusMonths(cycleMonths);
	}

	/** 임박 알림 대상인지. 검사 대상이면서 알림이 켜져 있어야 한다. */
	public boolean notifiable() {
		return enabled && notificationEnabled;
	}

	/**
	 * 검사 실시를 반영한다.
	 * 성적서 유효기간({@code validUntil})이 없으면 예정일 지정은 해제되고 주기 계산으로 돌아간다.
	 */
	public InspectionItem inspected(LocalDate inspectedAt, LocalDate validUntil) {
		return new InspectionItem(type, enabled, cycleMonths, inspectedAt, validUntil, notificationEnabled);
	}

	/**
	 * 수정 요청값을 덮어쓴다. null은 미전달로 보아 기존 값을 유지한다(도메인 {@code update()} 의 keep 시맨틱).
	 * 검사 종류는 이 항목의 식별자이므로 바뀌지 않는다.
	 */
	InspectionItem merge(InspectionItemChange change) {
		return new InspectionItem(
			this.type,
			change.enabled() != null ? change.enabled() : this.enabled,
			change.cycleMonths() != null ? change.cycleMonths() : this.cycleMonths,
			change.lastInspectedAt() != null ? change.lastInspectedAt() : this.lastInspectedAt,
			change.nextDueDateOverride() != null ? change.nextDueDateOverride() : this.nextDueDateOverride,
			change.notificationEnabled() != null ? change.notificationEnabled() : this.notificationEnabled
		);
	}
}
