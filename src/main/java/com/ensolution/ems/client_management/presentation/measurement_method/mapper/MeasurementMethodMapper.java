package com.ensolution.ems.client_management.presentation.measurement_method.mapper;

import com.ensolution.ems.client_management.application.command.create.CreateMeasurementMethodCommand;
import com.ensolution.ems.client_management.application.command.update.UpdateMeasurementMethodCommand;
import com.ensolution.ems.client_management.domain.MeasurementMethod;
import com.ensolution.ems.client_management.presentation.measurement_method.request.CreateMeasurementMethodRequest;
import com.ensolution.ems.client_management.presentation.measurement_method.request.UpdateMeasurementMethodRequest;
import com.ensolution.ems.client_management.presentation.measurement_method.response.MeasurementMethodResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder
)
public interface MeasurementMethodMapper {
	@Mapping(target = "tenantId", source = "tenantId")
	CreateMeasurementMethodCommand toCreateCommand(CreateMeasurementMethodRequest request, Long tenantId);

	UpdateMeasurementMethodCommand toUpdateCommand(UpdateMeasurementMethodRequest request);

	MeasurementMethodResponse toResponse(MeasurementMethod method);

	List<MeasurementMethodResponse> toResponses(List<MeasurementMethod> methods);
}
