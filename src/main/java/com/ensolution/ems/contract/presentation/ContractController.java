package com.ensolution.ems.contract.presentation;

import com.ensolution.ems.contract.application.service.ContractService;
import com.ensolution.ems.contract.application.command.ContractDetail;
import com.ensolution.ems.contract.presentation.mapper.ContractMapper;
import com.ensolution.ems.contract.presentation.request.CreateContractRequest;
import com.ensolution.ems.contract.presentation.request.UpdateContractRequest;
import com.ensolution.ems.contract.presentation.response.ContractListResponse;
import com.ensolution.ems.contract.presentation.response.ContractResponse;
import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Contract", description = "계약 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

	private final ContractService contractService;
	private final ContractMapper mapper;

	@Operation(summary = "계약 등록")
	@PostMapping()
	public ResponseEntity<ApiResponse<ContractResponse>> createContract(
		@Valid @RequestBody CreateContractRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		ContractDetail detail = contractService.createContract(mapper.toCreateCommand(request, principal.getTenantId()));
		return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(detail)));
	}

	@Operation(summary = "계약 목록 조회", description = "workplaceId 쿼리 파라미터로 사업장을 지정합니다.")
	@GetMapping()
	public ResponseEntity<ApiResponse<List<ContractListResponse>>> getContractList(
		@RequestParam(required = false) Long workplaceId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok(ApiResponse.success(mapper.toListResponses(
			contractService.getContractList(workplaceId, principal.getTenantId())
		)));
	}

	@Operation(summary = "계약 상세 조회")
	@GetMapping("/{contractId}")
	public ResponseEntity<ApiResponse<ContractResponse>> getContract(@PathVariable Long contractId) {
		ContractDetail detail = contractService.getContract(contractId);
		return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(detail)));
	}

	@Operation(summary = "계약 수정", description = "전달하지 않은 필드는 기존 값을 유지합니다.")
	@PutMapping("/{contractId}")
	public ResponseEntity<ApiResponse<ContractResponse>> updateContract(
		@PathVariable Long contractId,
		@RequestBody UpdateContractRequest request
	) {
		ContractDetail detail = contractService.updateContract(contractId, mapper.toUpdateCommand(request));
		return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(detail)));
	}

	@Operation(summary = "계약 삭제")
	@DeleteMapping("/{contractId}")
	public ResponseEntity<ApiResponse<Void>> deleteContract(@PathVariable Long contractId) {
		contractService.deleteContract(contractId);
		return ResponseEntity.ok(ApiResponse.success());
	}
}
