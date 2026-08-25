package com.ensolution.ems.dashboard.presentation.mapper;

import com.ensolution.ems.dashboard.application.command.DashboardOverview;
import com.ensolution.ems.dashboard.application.command.ExpiringContract;
import com.ensolution.ems.dashboard.application.command.InspectionDue;
import com.ensolution.ems.dashboard.application.command.MeasurementCountItem;
import com.ensolution.ems.dashboard.presentation.response.DashboardOverviewResponse;
import com.ensolution.ems.dashboard.presentation.response.ExpiringContractResponse;
import com.ensolution.ems.dashboard.presentation.response.InspectionDueResponse;
import com.ensolution.ems.dashboard.presentation.response.MeasurementCountChartResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * VO와 응답의 필드명이 1:1로 대응한다는 전제로 이름 기준 매핑을 쓴다.
 * 한쪽만 바꾸면 조용히 null이 나가므로 {@code unmappedTargetPolicy = ERROR} 로 컴파일 오류로 승격시킨다.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DashboardMapper {

	DashboardOverviewResponse toResponse(DashboardOverview overview);

	ExpiringContractResponse toResponse(ExpiringContract item);

	InspectionDueResponse toResponse(InspectionDue item);

	List<MeasurementCountChartResponse> toListResponses(List<MeasurementCountItem> items);
}
