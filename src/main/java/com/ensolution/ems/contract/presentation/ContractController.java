package com.ensolution.ems.contract.presentation;

import com.ensolution.ems.contract.application.ContractCommandService;
import com.ensolution.ems.contract.application.ContractQueryService;
import com.ensolution.ems.contract.application.command.ContractDetail;
import com.ensolution.ems.contract.presentation.mapper.ContractMapper;
import com.ensolution.ems.contract.presentation.request.CreateContractRequest;
import com.ensolution.ems.contract.presentation.request.UpdateContractRequest;
import com.ensolution.ems.contract.presentation.response.ContractListResponse;
import com.ensolution.ems.contract.presentation.response.ContractResponse;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Contract", description = "계약 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

	private final ContractCommandService contractCommandService;
	private final ContractQueryService contractQueryService;
	private final ContractMapper mapper;

	@Operation(summary = "계약 등록")
	@PostMapping()
	public ResponseEntity<ApiResponse<ContractResponse>> createContract(
		@Valid @RequestBody CreateContractRequest request
	) {
		ContractDetail detail = contractCommandService.createContract(mapper.toCreateCommand(request));
		return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(detail)));
	}

	@Operation(summary = "계약 목록 조회", description = "workplaceId 쿼리 파라미터로 사업장을 지정합니다.")
	@GetMapping()
	public ResponseEntity<ApiResponse<List<ContractListResponse>>> getContractList(
		@RequestParam(required = false) Long workplaceId
	) {
		return ResponseEntity.ok(ApiResponse.success(mapper.toListResponses(
			contractQueryService.getContractList(workplaceId)
		)));
	}

	@Operation(summary = "계약 상세 조회")
	@GetMapping("/{contractId}")
	public ResponseEntity<ApiResponse<ContractResponse>> getContract(@PathVariable Long contractId) {
		ContractDetail detail = contractQueryService.getContract(contractId);
		return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(detail)));
	}

	@Operation(summary = "계약 수정", description = "전달하지 않은 필드는 기존 값을 유지합니다.")
	@PutMapping("/{contractId}")
	public ResponseEntity<ApiResponse<ContractResponse>> updateContract(
		@PathVariable Long contractId,
		@RequestBody UpdateContractRequest request
	) {
		ContractDetail detail = contractCommandService.updateContract(contractId, mapper.toUpdateCommand(request));
		return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(detail)));
	}

	@Operation(summary = "계약 삭제")
	@DeleteMapping("/{contractId}")
	public ResponseEntity<ApiResponse<Void>> deleteContract(@PathVariable Long contractId) {
		contractCommandService.deleteContract(contractId);
		return ResponseEntity.ok(ApiResponse.success());
	}
}
