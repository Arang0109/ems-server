package com.ensolution.ems.client_management.presentation;

import com.ensolution.ems.client_management.application.CompanyService;
import com.ensolution.ems.client_management.domain.Company;
import com.ensolution.ems.client_management.presentation.mapper.CompanyMapper;
import com.ensolution.ems.client_management.presentation.request.create.CreateCompanyRequest;
import com.ensolution.ems.client_management.presentation.request.update.UpdateCompanyRequest;
import com.ensolution.ems.client_management.presentation.response.CompanyResponse;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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
	private final CompanyMapper mapper;
	
	@Operation(summary = "의뢰기관 등록")
	@PostMapping()
	public ResponseEntity<ApiResponse<CompanyResponse>> createCompany(
		@Valid @RequestBody CreateCompanyRequest request
	) {
		Company savedCompany = companyService.createCompany(mapper.toCreateCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(savedCompany)));
	}
	
	@Operation(summary = "의뢰기관 목록 조회")
	@GetMapping()
	public ResponseEntity<ApiResponse<List<CompanyResponse>>> getCompanyList() {
		List<Company> companies = companyService.getCompanyList();
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponses(companies)));
	}
	
	@Operation(summary = "의뢰기관 상세 조회")
	@GetMapping("/{companyId}")
	public ResponseEntity<ApiResponse<CompanyResponse>> getCompany(
		@PathVariable Long companyId
	) {
		Company company = companyService.getCompany(companyId);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(company)));
	}
	
	@Operation(summary = "의뢰기관 수정", description = "전달하지 않은 필드는 기존 값을 유지합니다.")
	@PutMapping("/{companyId}")
	public ResponseEntity<ApiResponse<CompanyResponse>> updateCompany(
		@PathVariable Long companyId,
		@RequestBody UpdateCompanyRequest request
		) {
		Company modifiedCompany = companyService.updateCompany(companyId, mapper.toUpdateCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(modifiedCompany)));
	}
	
	@Operation(summary = "의뢰기관 삭제")
	@DeleteMapping("/{companyId}")
	public ResponseEntity<ApiResponse<Void>> deleteCompany(@PathVariable Long companyId) {
		companyService.deleteCompany(companyId);
		return ResponseEntity.ok().body(ApiResponse.success());
	}
}