package com.ensolution.ems.client_management.presentation;

import com.ensolution.ems.client_management.application.CompanyService;
import com.ensolution.ems.client_management.domain.Company;
import com.ensolution.ems.client_management.presentation.mapper.CompanyPresentationMapper;
import com.ensolution.ems.client_management.presentation.request.CreateCompanyRequest;
import com.ensolution.ems.client_management.presentation.request.UpdateCompanyRequest;
import com.ensolution.ems.client_management.presentation.response.CompanyResponse;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Company", description = "측정대행 의뢰기관 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {
	
	private final CompanyService companyService;
	private final CompanyPresentationMapper mapper;
	
	@PostMapping()
	public ResponseEntity<ApiResponse<CompanyResponse>> createCompany(
		@Valid @RequestBody CreateCompanyRequest request
	) {
		System.out.println(request.toString());
		Company savedCompany = companyService.createCompany(mapper.toCreateCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(savedCompany)));
	}
	
	@GetMapping()
	public ResponseEntity<ApiResponse<List<CompanyResponse>>> getCompanyList() {
		List<Company> companies = companyService.getCompanyList();
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponses(companies)));
	}
	
	@GetMapping("/{companyId}")
	public ResponseEntity<ApiResponse<CompanyResponse>> getCompanyDetail(
		@PathVariable Long companyId
	) {
		Company company = companyService.getCompany(companyId);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(company)));
	}
	
	@PutMapping("{companyId}")
	public ResponseEntity<ApiResponse<CompanyResponse>> updateCompany(
		@PathVariable Long companyId,
		@RequestBody UpdateCompanyRequest request
		) {
		Company modifiedCompany = companyService.updateCompany(companyId, mapper.toUpdateCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(modifiedCompany)));
	}
	
	@DeleteMapping("/{companyId}")
	public ResponseEntity<ApiResponse<Void>> deleteCompany(@PathVariable Long companyId) {
		companyService.deleteCompany(companyId);
		return ResponseEntity.ok().body(ApiResponse.success());
	}
}