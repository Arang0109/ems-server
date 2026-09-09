package com.ensolution.ems.schedule.domain.snapshot;

import com.ensolution.ems.schedule.domain.sampling.SamplingSheet;

import java.time.LocalTime;
import java.util.List;

/**
 * 그 회차의 현장 채취 사실을 담는 스냅샷 — 언제 채취했고, 누가 현장에 있었으며, 무엇을 적었는가.
 *
 * <p>여기 있는 값은 <b>원장에 없는 회차 고유값</b>이다. 그래서 원장 사본인
 * {@link WorkplaceSnapshot}·{@link TenantSnapshot}이 아니라 이 노드가 갖는다 —
 * 사업장 원장에는 "그날 누가 입회했는가"라는 칸이 없다.
 *
 * <p>측정항목({@code items})은 여기 두지 않는다. 항목은 채취 산출물이 아니라 성적서 항목 축이고
 * 이행 이력이 파생되는 근거이며, 편집 경로도 시트와 따로다({@code changeItems}·{@code reorderItems}).
 * 그래서 {@link ScheduleSnapshot} 루트에 남는다.
 */
public record SamplingSnapshot(
	LocalTime samplingStartedAt,     // 채취시작시각
	LocalTime samplingEndedAt,       // 채취종료시각

	String facilityManager,          // 배출시설관리자
	String samplingWitness,          // 시료채취입회자(환경기술인)

	List<SamplingSheet> sheets       // 채취 기록지
) {

	/**
	 * 측정계획 생성 시점의 채취 스냅샷을 만든다. 현장 담당자는 사업장 원장에서 복사해 초기값으로
	 * 깔아 두고, 실제로 그날 다른 사람이 입회했다면 {@link #update}로 이 회차만 고친다.
	 * 채취 시각과 기록지는 아직 없다.
	 */
	public static SamplingSnapshot create(String facilityManager, String samplingWitness) {
		return new SamplingSnapshot(null, null, facilityManager, samplingWitness, List.of());
	}

	/** 기록지만 교체한 새 채취 스냅샷을 반환한다. */
	public SamplingSnapshot withSheets(List<SamplingSheet> newSheets) {
		return new SamplingSnapshot(
			samplingStartedAt, samplingEndedAt, facilityManager, samplingWitness, newSheets);
	}

	/**
	 * 채취 시각과 현장 담당자를 갱신한 새 스냅샷을 반환한다.
	 * 전달되지 않은(공백 포함) 값은 기존 값을 유지하는 <b>부분 갱신</b>이다 — 성적서 기본정보 폼이
	 * 칸을 나눠 채우는 경로라, 비어 온 칸을 "지웠다"로 읽으면 다른 칸만 고쳐도 나머지가 날아간다.
	 */
	public SamplingSnapshot update(
		LocalTime newStartedAt, LocalTime newEndedAt,
		String newFacilityManager, String newSamplingWitness
	) {
		return new SamplingSnapshot(
			SnapshotMerge.keep(newStartedAt, samplingStartedAt),
			SnapshotMerge.keep(newEndedAt, samplingEndedAt),
			SnapshotMerge.keepText(newFacilityManager, facilityManager),
			SnapshotMerge.keepText(newSamplingWitness, samplingWitness),
			sheets);
	}
}
