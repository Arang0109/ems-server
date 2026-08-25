package com.ensolution.ems.schedule.presentation.history.mapper;

import com.ensolution.ems.schedule.application.command.detail.FulfillmentBoardDetail;
import com.ensolution.ems.schedule.application.command.list_item.MeasurementRecordListItem;
import com.ensolution.ems.schedule.application.command.list_item.PendingMeasurementListItem;
import com.ensolution.ems.schedule.presentation.history.response.FulfillmentBoardResponse;
import com.ensolution.ems.schedule.presentation.history.response.MeasurementRecordListResponse;
import com.ensolution.ems.schedule.presentation.history.response.PendingMeasurementListResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * VO와 응답 DTO의 필드를 1:1로 유지한다. {@code unmappedTargetPolicy = ERROR}이므로 한쪽에만 필드를 더하면
 * 조용히 null이 나가는 대신 빌드가 깨진다({@code DashboardMapper}와 같은 방침).
 */
@Mapper(
	componentModel = "spring",
	unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface MeasurementHistoryMapper {

	MeasurementRecordListResponse toListResponse(MeasurementRecordListItem item);

	List<MeasurementRecordListResponse> toListResponses(List<MeasurementRecordListItem> items);

	PendingMeasurementListResponse toPendingListResponse(PendingMeasurementListItem item);

	List<PendingMeasurementListResponse> toPendingListResponses(List<PendingMeasurementListItem> items);

	FulfillmentBoardResponse toBoardResponse(FulfillmentBoardDetail detail);

	FulfillmentBoardResponse.Row toRowResponse(FulfillmentBoardDetail.Row row);

	FulfillmentBoardResponse.Cell toCellResponse(FulfillmentBoardDetail.Cell cell);
}
