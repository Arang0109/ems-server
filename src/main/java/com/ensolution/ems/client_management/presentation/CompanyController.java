package com.ensolution.ems.client_management.presentation;

import com.ensolution.ems.client_management.application.CompanyService;
import com.ensolution.ems.client_management.domain.Company;
import com.ensolution.ems.client_management.presentation.mapper.CompanyPresentationMapper;
import com.ensolution.ems.client_management.presentation.request.CreateCompanyRequest;
import com.ensolution.ems.client_management.presentation.response.CompanyResponse;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Company", description = "측정대행 의뢰기관 API")
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {
	
	private final CompanyService companyService;
	private final CompanyPresentationMapper mapper;
	
	@PostMapping()
	public ResponseEntity<ApiResponse<CompanyResponse>> createCompany(
		@RequestBody CreateCompanyRequest request
	) {
		Company company = companyService.createCompany(mapper.toCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(company)));
	}
	
	@GetMapping()
	public ResponseEntity<ApiResponse<List<CompanyResponse>>> getCompanyList() {
		List<Company> companies = companyService.getCompanyList();
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponses(companies)));
	}
}
