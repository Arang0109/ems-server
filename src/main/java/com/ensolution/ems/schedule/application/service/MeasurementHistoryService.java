package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.command.detail.FulfillmentBoardDetail;
import com.ensolution.ems.schedule.application.command.list_item.MeasurementRecordListItem;
import com.ensolution.ems.schedule.application.command.list_item.PendingMeasurementListItem;
import com.ensolution.ems.schedule.application.port.out.MeasurementRecordRepository;
import com.ensolution.ems.schedule.domain.history.MeasurementPeriod;
import com.ensolution.ems.schedule.domain.history.MeasurementRecord;
import com.ensolution.ems.schedule.domain.history.MeasurementResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 측정 이력 조회 유스케이스. 이력을 만드는 쪽은 {@link MeasurementRecordRecorder}이고 여기는 읽기만 한다 —
 * 쓰기는 측정계획 완료라는 다른 유스케이스에 종속된 부수효과이므로 한 서비스에 섞지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeasurementHistoryService {

	private final MeasurementRecordRepository measurementRecordRepository;
	private final FulfillmentBoardAssembler fulfillmentBoardAssembler;

	/** 측정지점의 회차별 이력. 연도를 주지 않으면 전체 기간을 반환한다. */
	public List<MeasurementRecordListItem> getRecords(Long stackId, Long tenantId, Integer year) {
		List<MeasurementRecord> records = year == null
			? measurementRecordRepository.findByStackId(stackId, tenantId)
			: measurementRecordRepository.findByStackIdAndYear(stackId, tenantId, year);

		return records.stream().map(this::toListItem).toList();
	}

	/** 연간 주기 이행 현황판. */
	public FulfillmentBoardDetail getFulfillmentBoard(
		Long tenantId, Long workplaceId, Long stackId, Integer year) {

		LocalDate today = LocalDate.now();
		return fulfillmentBoardAssembler.assemble(
			tenantId, workplaceId, stackId, resolveYear(year, today), today);
	}

	/** 기한이 지났거나 임박한 미이행 구간. */
	public List<PendingMeasurementListItem> getPendingMeasurements(
		Long tenantId, Integer year, int withinDays) {

		LocalDate today = LocalDate.now();
		return fulfillmentBoardAssembler.assemblePending(
			tenantId, resolveYear(year, today), today, withinDays);
	}

	private int resolveYear(Integer year, LocalDate today) {
		return year == null ? today.getYear() : year;
	}

	/** 주기 구간 표기는 저장값이 아니라 파생값이므로 조회 시 도메인에서 다시 만든다. */
	private MeasurementRecordListItem toListItem(MeasurementRecord record) {
		MeasurementPeriod period = record.period();
		MeasurementResult result = record.result() == null ? MeasurementResult.empty() : record.result();

		return new MeasurementRecordListItem(
			record.id(),
			record.scheduleId(),
			record.sampledAt(),
			record.pollutantId(),
			record.pollutantCode(),
			record.pollutantNameKr(),
			record.cycle(),
			period == null ? null : period.key(),
			period == null ? null : period.label(),
			result.concentration(),
			result.unit(),
			result.correctedConcentration(),
			result.emission(),
			result.allowance(),
			result.exceeded());
	}
}
