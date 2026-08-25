package com.ensolution.ems.schedule.domain.snapshot;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 측정계획 세부 스냅샷 애그리거트(MongoDB 문서 본문의 도메인 표현).
 * 측정 시점의 대상·팀·장비·측정항목 정보를 복사해 불변으로 보관하므로,
 * 원장(tenant·equipment)이 변경되어도 과거 기록은 영향을 받지 않는다.
 * {@code sheets}는 실제 측정값과 계산 결과를 담는 측정 시트 목록이다.
 * <p>
 * {@code version}·{@code createdAt}은 문서의 저장 메타다. 도메인이 들고 다녀야
 * 읽어온 문서의 값이 저장까지 이어져 낙관적 락이 성립하고(version), 저장할 때마다
 * 생성 시각이 지워지지 않는다(createdAt). 값을 만들어내는 것은 인프라(Spring Data)다.
 */
public record ScheduleSnapshot(
	String id,             // Mongo _id (= scheduleId 문자열)
	Long scheduleId,       // MySQL 메타 PK 연결키
	Long tenantId,
	ScheduleStatus status,
	BasicInfo basicInfo,
	TeamSnapshot team,
	TenantSnapshot tenant,
	ClientSnapshot client,
	List<EquipmentSnapshot> equipments,
	List<SamplingItemSnapshot> items,
	List<MeasurementSheet> sheets,
	Long version,
	LocalDateTime createdAt
) {
	
	public ScheduleSnapshot syncStatus(ScheduleStatus next) {
		return new ScheduleSnapshot(id, scheduleId, tenantId, next, basicInfo, team, tenant, client, equipments, items, sheets, version, createdAt);
	}

	public ScheduleSnapshot withSheets(List<MeasurementSheet> newSheets) {
		return new ScheduleSnapshot(id, scheduleId, tenantId, status, basicInfo, team, tenant, client, equipments, items, newSheets, version, createdAt);
	}

	public ScheduleSnapshot applyEquipmentChange(TeamSnapshot newTeam,
	                                             List<EquipmentSnapshot> newEquipments,
	                                             List<MeasurementSheet> newSheets) {
		return new ScheduleSnapshot(id, scheduleId, tenantId, status, basicInfo,
			newTeam, tenant, client, newEquipments, items, newSheets, version, createdAt);
	}

	public ScheduleSnapshot applyClientChange(ClientSnapshot patch, List<MeasurementSheet> newSheets) {
		return new ScheduleSnapshot(id, scheduleId, tenantId, status, basicInfo, team, tenant,
			client == null ? patch : client.merge(patch),
			equipments, items, newSheets, version, createdAt);
	}

	public ScheduleSnapshot withItems(List<SamplingItemSnapshot> newItems) {
		return new ScheduleSnapshot(id, scheduleId, tenantId, status, basicInfo, team, tenant, client,
			equipments, newItems, sheets, version, createdAt);
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

	public ScheduleSnapshot withItemReplaced(Long pollutantId, SamplingItemSnapshot replacement) {
		List<SamplingItemSnapshot> replaced = items.stream()
			.map(item -> item != null && pollutantId.equals(item.pollutantId()) ? replacement : item)
			.toList();
		return withItems(replaced);
	}

	public ScheduleSnapshot applyBasicInfo(BasicInfo newBasicInfo, TeamSnapshot newTeam) {
		return new ScheduleSnapshot(id, scheduleId, tenantId, status, newBasicInfo,
			newTeam, tenant, client, equipments, items, sheets, version, createdAt);
	}

	/**
	 * 메타 수정 결과를 문서에 반영한다. basicInfo·referenceNumber를 갱신하며, client 트리는 변경하지 않는다
	 * (의뢰기관·사업장·측정시설 스냅샷 수정은 {@link #applyClientChange} 경로를 사용한다).
	 */
	public ScheduleSnapshot applyMetaUpdate(BasicInfo newBasicInfo, TenantSnapshot newTenant) {
		return new ScheduleSnapshot(
			id, scheduleId, tenantId,
			status, newBasicInfo, team, newTenant,
			client,
			equipments, items, sheets, version, createdAt);
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
