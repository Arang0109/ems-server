package com.ensolution.ems.schedule.domain.snapshot;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.domain.sampling.SamplingSheet;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>{@code version}은 문서 단위 낙관적 락 토큰이다. 읽어온 값이 저장까지 이어져야 락이 성립하므로
 * 도메인이 들고 다니며, 값을 만들어내는 것은 인프라(Spring Data)다. 문서 생성 시각({@code createdAt})은
 * 측정 사실이 아니라 저장 메타라 도메인에 두지 않고 {@code ScheduleDocument}에만 남긴다.
 */
public record ScheduleSnapshot(
	String id,             // Mongo _id (= scheduleId 문자열)
	Long scheduleId,       // MySQL 메타 PK 연결키
	Long tenantId,
	Long version,          // 문서 단위 낙관적 락 토큰

	ClientSnapshot client,
	TenantSnapshot tenant,
	TeamSnapshot team,
	SamplingSnapshot samplingData,

	List<SamplingItemSnapshot> items
) {
	
	public List<SamplingSheet> sheets() { 					// 채취 기록지 목록
		return samplingData == null || samplingData.sheets() == null ? List.of() : samplingData.sheets();
	}
	
	public List<EquipmentSnapshot> equipments() { 	// 이 회차에 쓴 측정장비
		return team == null || team.equipments() == null ? List.of() : team.equipments();
	}

	/**
	 * 기록지만 교체한 새 스냅샷을 반환한다.
	 * <p>
	 * 인자가 하나인 fluent 메서드는 MapStruct가 property setter로 읽으므로,
	 * {@code ScheduleDocumentMapper} 에서 매핑 대상 제외를 함께 선언해야 한다.
	 */
	public ScheduleSnapshot withSheets(List<SamplingSheet> newSheets) {
		SamplingSnapshot sampling = samplingData == null
			? SamplingSnapshot.create(null, null).withSheets(newSheets)
			: samplingData.withSheets(newSheets);
		return new ScheduleSnapshot(id, scheduleId, tenantId, version, client, tenant, team, sampling, items);
	}

	/** 채취 스냅샷(채취시각·현장 담당자·기록지)을 통째로 교체한 새 스냅샷을 반환한다. */
	public ScheduleSnapshot withSampling(SamplingSnapshot newSampling) {
		return new ScheduleSnapshot(id, scheduleId, tenantId, version, client, tenant, team, newSampling, items);
	}

	/**
	 * 장비 교체 결과를 반영한다. 장비는 팀 스냅샷 안에 있으므로 새 팀만 받으면 되고,
	 * 장비가 바뀌면 계산 입력(피토관 계수·노즐경 등)이 달라지므로 재계산된 기록지를 함께 받는다.
	 */
	public ScheduleSnapshot applyEquipmentChange(TeamSnapshot newTeam, List<SamplingSheet> newSheets) {
		return new ScheduleSnapshot(id, scheduleId, tenantId, version, client, tenant, newTeam,
			samplingData == null ? SamplingSnapshot.create(null, null).withSheets(newSheets)
				: samplingData.withSheets(newSheets),
			items);
	}

	/**
	 * 의뢰기관 트리를 병합한다. 굴뚝 형상·치수가 계산 입력이므로 재계산된 기록지를 함께 받는다.
	 */
	public ScheduleSnapshot applyClientChange(ClientSnapshot patch, List<SamplingSheet> newSheets) {
		return new ScheduleSnapshot(id, scheduleId, tenantId, version,
			client == null ? patch : client.merge(patch),
			tenant, team,
			samplingData == null ? SamplingSnapshot.create(null, null).withSheets(newSheets)
				: samplingData.withSheets(newSheets),
			items);
	}

	/**
	 * 측정항목 목록만 교체한 새 스냅샷을 반환한다.
	 * <p>
	 * 인자가 하나인 fluent 메서드는 MapStruct가 property setter로 읽으므로,
	 * {@code ScheduleDocumentMapper} 에서 매핑 대상 제외를 함께 선언해야 한다.
	 */
	public ScheduleSnapshot withItems(List<SamplingItemSnapshot> newItems) {
		return new ScheduleSnapshot(id, scheduleId, tenantId, version, client, tenant, team, samplingData, newItems);
	}

	/**
	 * 성적서 서명란 담당자와 측정자 표기를 갱신한 새 스냅샷을 반환한다.
	 * 둘 다 원장이 기본값을 갖지만 회차별로 다를 수 있어 이 문서만 고친다 — 원장은 건드리지 않는다.
	 */
	public ScheduleSnapshot applyStaff(TenantSnapshot newTenant, TeamSnapshot newTeam) {
		return new ScheduleSnapshot(id, scheduleId, tenantId, version, client, newTenant, newTeam, samplingData, items);
	}

	/**
	 * 측정항목을 주어진 측정물질 순서대로 재배치한 새 스냅샷을 반환한다. 항목의 내용은 그대로 두고 순서만 바꾸며,
	 * 계산 입력이 아니므로 시트도 재계산하지 않는다.
	 * <p>
	 * <b>이 순서가 곧 성적서의 항목 순서다.</b> 기록부 서식은 한 장에 실을 수 있는 항목 수가 정해져 있어
	 * 템플릿이 {@code ${items[0].name}} 처럼 인덱스로 칸을 지목하므로, 몇 번째 항목이 몇 번째 장
	 * 어느 칸에 들어갈지를 이 배열 순서가 결정한다.
	 * <p>
	 * 인자는 이 계획의 측정항목 <b>전체</b>여야 한다 — 부분 목록은 나머지 항목의 자리를 정의하지 못한다.
	 * 집합 일치는 호출 전에 검증하며({@code ScheduleValidator#requireExactItemOrder}), 여기서는
	 * 검증을 통과한 목록임을 전제로 재배치만 한다.
	 * <p>
	 * 인자가 하나인 fluent 메서드는 MapStruct가 property setter로 읽으므로,
	 * {@code ScheduleDocumentMapper} 에서 매핑 대상 제외를 함께 선언해야 한다.
	 */
	public ScheduleSnapshot withItemOrder(List<Long> orderedPollutantIds) {
		Map<Long, SamplingItemSnapshot> byPollutantId = (items == null ? List.<SamplingItemSnapshot>of() : items)
			.stream()
			.collect(Collectors.toMap(SamplingItemSnapshot::pollutantId, Function.identity(), (a, b) -> a));

		return withItems(orderedPollutantIds.stream()
			.map(byPollutantId::get)
			.filter(Objects::nonNull)
			.toList());
	}

	/**
	 * 같은 측정물질의 항목을 <b>제자리에서</b> 바꾼 새 스냅샷을 반환한다.
	 * 배열 순서가 곧 성적서 칸 배치이므로, 지웠다 붙이는 방식으로 바꾸면 손댄 항목이 맨 뒤로 밀려
	 * 성적서 칸이 어긋난다.
	 */
	public ScheduleSnapshot withItemReplaced(Long pollutantId, SamplingItemSnapshot replacement) {
		List<SamplingItemSnapshot> replaced = (items == null ? List.<SamplingItemSnapshot>of() : items).stream()
			.map(item -> pollutantId.equals(item.pollutantId()) ? replacement : item)
			.toList();
		return withItems(replaced);
	}

	/**
	 * 이번 계획의 측정항목 중 해당 측정물질을 찾는다. 계획에 없는 물질이면 예외를 던진다 —
	 * 실험분석정보처럼 항목에 딸린 기록이 계획 밖의 물질로 만들어지는 것을 막는다.
	 */
	public SamplingItemSnapshot requireItem(Long pollutantId) {
		if (items == null || pollutantId == null) {
			throw new CustomException(ErrorCode.SCHEDULE_ITEM_NOT_IN_SCHEDULE);
		}
		return items.stream()
			.filter(item -> item != null && pollutantId.equals(item.pollutantId()))
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_ITEM_NOT_IN_SCHEDULE));
	}
}
