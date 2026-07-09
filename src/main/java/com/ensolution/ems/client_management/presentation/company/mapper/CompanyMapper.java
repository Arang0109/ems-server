package com.ensolution.ems.client_management.presentation.company.mapper;

import com.ensolution.ems.client_management.application.command.create.CreateCompanyCommand;
import com.ensolution.ems.client_management.application.command.update.UpdateCompanyCommand;
import com.ensolution.ems.client_management.domain.Company;
import com.ensolution.ems.client_management.presentation.company.response.CompanyResponse;
import com.ensolution.ems.client_management.presentation.company.request.CreateCompanyRequest;
import com.ensolution.ems.client_management.presentation.company.request.UpdateCompanyRequest;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(
		componentModel = "spring",
		builder = @Builder
)
public interface CompanyMapper {
	CreateCompanyCommand toCreateCommand(CreateCompanyRequest request);
	UpdateCompanyCommand toUpdateCommand(UpdateCompanyRequest request);
	CompanyResponse toResponse(Company company);
	List<CompanyResponse> toResponses(List<Company> companies);
}
