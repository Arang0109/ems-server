package com.ensolution.ems.client_management.presentation.mapper;

import com.ensolution.ems.client_management.application.command.CreateCompanyCommand;
import com.ensolution.ems.client_management.application.command.UpdateCompanyCommand;
import com.ensolution.ems.client_management.domain.Company;
import com.ensolution.ems.client_management.presentation.request.CreateCompanyRequest;
import com.ensolution.ems.client_management.presentation.request.UpdateCompanyRequest;
import com.ensolution.ems.client_management.presentation.response.CompanyResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(
		componentModel = "spring",
		builder = @Builder
)
public interface CompanyPresentationMapper {
	CreateCompanyCommand toCreateCommand(CreateCompanyRequest request);
	UpdateCompanyCommand toUpdateCommand(UpdateCompanyRequest request);
	CompanyResponse toResponse(Company company);
	List<CompanyResponse> toResponses(List<Company> companies);
}
