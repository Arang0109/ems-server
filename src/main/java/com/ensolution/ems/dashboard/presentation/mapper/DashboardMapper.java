package com.ensolution.ems.dashboard.presentation.mapper;

import com.ensolution.ems.dashboard.application.command.DashboardOverview;
import com.ensolution.ems.dashboard.application.command.MeasurementCountItem;
import com.ensolution.ems.dashboard.presentation.response.DashboardOverviewResponse;
import com.ensolution.ems.dashboard.presentation.response.MeasurementCountChartResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DashboardMapper {

	DashboardOverviewResponse toResponse(DashboardOverview overview);

	List<MeasurementCountChartResponse> toListResponses(List<MeasurementCountItem> items);
}
