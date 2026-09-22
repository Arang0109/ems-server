package com.ensolution.ems.schedule.presentation.custom_field.mapper;

import com.ensolution.ems.schedule.application.command.create.CreateCustomFieldDefinitionCommand;
import com.ensolution.ems.schedule.application.command.update.UpdateCustomFieldDefinitionCommand;
import com.ensolution.ems.schedule.domain.custom_field.CustomFieldDefinition;
import com.ensolution.ems.schedule.presentation.custom_field.request.CreateCustomFieldDefinitionRequest;
import com.ensolution.ems.schedule.presentation.custom_field.request.UpdateCustomFieldDefinitionRequest;
import com.ensolution.ems.schedule.presentation.custom_field.response.CustomFieldDefinitionResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder
)
public interface CustomFieldDefinitionMapper {
	@Mapping(target = "tenantId", source = "tenantId")
	CreateCustomFieldDefinitionCommand toCreateCommand(CreateCustomFieldDefinitionRequest request, Long tenantId);

	UpdateCustomFieldDefinitionCommand toUpdateCommand(UpdateCustomFieldDefinitionRequest request);

	CustomFieldDefinitionResponse toResponse(CustomFieldDefinition definition);

	List<CustomFieldDefinitionResponse> toResponses(List<CustomFieldDefinition> definitions);
}
