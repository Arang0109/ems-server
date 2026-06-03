package com.ensolution.ems.client_management.presentation.mapper;

import com.ensolution.ems.client_management.application.command.CreateCompanyCommand;
import com.ensolution.ems.client_management.domain.Company;
import com.ensolution.ems.client_management.presentation.request.CreateCompanyRequest;
import com.ensolution.ems.client_management.presentation.response.CompanyResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(
		componentModel = "spring",
		builder = @Builder
)
public interface CompanyPresentationMapper {
	CreateCompanyCommand toCommand(CreateCompanyRequest request);
	CompanyResponse toResponse(Company company);
	List<CompanyResponse> toResponses(List<Company> companies);
}
