package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.client_management.application.port.in.StackMeasurementItemSummary;
import com.ensolution.ems.client_management.application.port.in.StackQueryUseCase;
import com.ensolution.ems.schedule.application.command.detail.FulfillmentBoardDetail;
import com.ensolution.ems.schedule.application.command.list_item.PendingMeasurementListItem;
import com.ensolution.ems.schedule.application.port.out.MeasurementRecordRepository;
import com.ensolution.ems.schedule.domain.history.FulfillmentStatus;
import com.ensolution.ems.schedule.domain.history.MeasurementPeriod;
import com.ensolution.ems.schedule.domain.history.MeasurementRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 주기 이행 현황판을 조립한다.
 *
 * <p>행 축(측정항목)은 원장({@code client_management})에서, 셀 값(이행 사실)은 이 모듈의 이력에서 온다.
 * <b>한 번도 측정하지 않은 항목도 행으로 나와야</b> 하므로 축을 이력에서 뽑을 수 없다 — 그러면 미이행 항목이
 * 애초에 화면에 등장하지 않아 현황판의 존재 이유가 사라진다.
 *
 * <p>조회는 <b>소스당 1회씩 총 2회로 고정</b>한다. 항목 수만큼 조회하지 않는다
 * ({@code PollutantCatalogAssembler}·{@code TeamAssembler}가 쓰는 in-memory 대조 규칙과 같다).
 * 원장과 이력이 서로 다른 모듈 소유라 물리 조인을 쓸 수 없으므로, 두 소스를 한 번씩 읽고 메모리에서 결합한다.
 */
@Component
@RequiredArgsConstructor
public class FulfillmentBoardAssembler {

	private final StackQueryUseCase stackQueryUseCase;
	private final MeasurementRecordRepository measurementRecordRepository;

	/**
	 * 연간 현황판을 만든다.
	 *
	 * @param baseDate 이행 기한 경과 판정의 기준일(보통 오늘)
	 */
	public FulfillmentBoardDetail assemble(
		Long tenantId, Long workplaceId, Long stackId, int year, LocalDate baseDate) {

		List<StackMeasurementItemSummary> items =
			stackQueryUseCase.findMeasurementItems(tenantId, workplaceId, stackId);
		Map<String, List<MeasurementRecord>> recordsByCell = indexByCell(loadRecords(tenantId, stackId, year));

		List<FulfillmentBoardDetail.Row> rows = items.stream()
			.filter(item -> item.cycle() != null)
			.map(item -> toRow(item, year, baseDate, recordsByCell))
			.toList();

		int unscheduled = (int) items.stream().filter(item -> item.cycle() == null).count();
		return new FulfillmentBoardDetail(year, unscheduled, rows);
	}

	/**
	 * 현황판에서 조치가 필요한 셀만 뽑아 기한 임박순으로 펼친다.
	 * 기한이 지난 구간이 목록 앞에 오도록 남은 일수 오름차순으로 정렬한다(경과 건은 음수).
	 *
	 * @param withinDays 앞으로 이 일수 안에 기한이 닿는 구간까지 포함한다. 기한이 지난 구간은 항상 포함한다
	 */
	public List<PendingMeasurementListItem> assemblePending(
		Long tenantId, int year, LocalDate baseDate, int withinDays) {

		FulfillmentBoardDetail board = assemble(tenantId, null, null, year, baseDate);

		return board.rows().stream()
			.flatMap(row -> row.cells().stream()
				.filter(cell -> cell.status().needsAction())
				.filter(cell -> ChronoUnit.DAYS.between(baseDate, cell.dueDate()) <= withinDays)
				.map(cell -> toPendingItem(row, cell, baseDate)))
			.sorted(Comparator.comparingLong(PendingMeasurementListItem::daysRemaining))
			.toList();
	}

	private List<MeasurementRecord> loadRecords(Long tenantId, Long stackId, int year) {
		return stackId == null
			? measurementRecordRepository.findByTenantIdAndYear(tenantId, year)
			: measurementRecordRepository.findByStackIdAndYear(stackId, tenantId, year);
	}

	/**
	 * 이력을 셀 단위로 묶는다. 키에 주기 표식이 담긴 {@code periodKey}를 쓰므로, 원장의 주기가 도중에
	 * 바뀌면 과거 기록이 새 주기의 칸에 붙지 않는다 — 분기로 바꾼 뒤 옛 월별 기록이 1분기를 채우는 일이 없다.
	 */
	private Map<String, List<MeasurementRecord>> indexByCell(List<MeasurementRecord> records) {
		return records.stream()
			.filter(MeasurementRecord::countsTowardFulfillment)
			.collect(Collectors.groupingBy(
				record -> cellKey(record.stackId(), record.pollutantId(), record.period().key())));
	}

	private FulfillmentBoardDetail.Row toRow(
		StackMeasurementItemSummary item, int year, LocalDate baseDate,
		Map<String, List<MeasurementRecord>> recordsByCell) {

		List<FulfillmentBoardDetail.Cell> cells = MeasurementPeriod.allIn(item.cycle(), year).stream()
			.map(period -> toCell(item, period, baseDate, recordsByCell))
			.toList();

		return new FulfillmentBoardDetail.Row(
			item.workplaceId(), item.workplaceName(),
			item.stackId(), item.stackName(),
			item.stackPollutantId(), item.pollutantId(), item.code(), item.nameKr(),
			item.cycle(),
			cells.stream().mapToInt(FulfillmentBoardDetail.Cell::requiredCount).sum(),
			cells.stream().mapToInt(FulfillmentBoardDetail.Cell::fulfilledCount).sum(),
			cells);
	}

	private FulfillmentBoardDetail.Cell toCell(
		StackMeasurementItemSummary item, MeasurementPeriod period, LocalDate baseDate,
		Map<String, List<MeasurementRecord>> recordsByCell) {

		List<MeasurementRecord> matched = recordsByCell.getOrDefault(
			cellKey(item.stackId(), item.pollutantId(), period.key()), List.of());

		return new FulfillmentBoardDetail.Cell(
			period.index(),
			period.key(),
			period.label(),
			period.endDate(),
			period.requiredCount(),
			matched.size(),
			FulfillmentStatus.of(period, matched.size(), baseDate),
			matched.stream().map(MeasurementRecord::scheduleId).distinct().toList());
	}

	private PendingMeasurementListItem toPendingItem(
		FulfillmentBoardDetail.Row row, FulfillmentBoardDetail.Cell cell, LocalDate baseDate) {

		return new PendingMeasurementListItem(
			row.workplaceId(), row.workplaceName(),
			row.stackId(), row.stackName(),
			row.stackPollutantId(), row.pollutantId(), row.code(), row.nameKr(),
			row.cycle(),
			cell.key(), cell.label(), cell.dueDate(),
			ChronoUnit.DAYS.between(baseDate, cell.dueDate()),
			cell.requiredCount(), cell.fulfilledCount(), cell.status());
	}

	private String cellKey(Long stackId, Long pollutantId, String periodKey) {
		return stackId + ":" + pollutantId + ":" + periodKey;
	}
}
