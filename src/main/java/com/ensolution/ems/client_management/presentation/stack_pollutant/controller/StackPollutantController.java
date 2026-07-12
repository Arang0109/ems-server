package com.ensolution.ems.client_management.presentation.stack_pollutant.controller;

import com.ensolution.ems.client_management.application.service.StackPollutantService;
import com.ensolution.ems.client_management.domain.StackPollutant;
import com.ensolution.ems.client_management.presentation.stack_pollutant.mapper.StackPollutantMapper;
import com.ensolution.ems.client_management.presentation.stack_pollutant.request.CreateStackPollutantRequest;
import com.ensolution.ems.client_management.presentation.stack_pollutant.response.StackPollutantResponse;
import com.ensolution.ems.client_management.presentation.stack_pollutant.response.StackPollutantTableListResponse;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "StackPollutant", description = "시설별 측정물질 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/stack-pollutants")
@RequiredArgsConstructor
public class StackPollutantController {

	private final StackPollutantService stackPollutantService;
	private final StackPollutantMapper mapper;

	@Operation(summary = "시설별 측정물질 등록")
	@PostMapping
	public ResponseEntity<ApiResponse<StackPollutantResponse>> registerPollutant(
		@Valid @RequestBody CreateStackPollutantRequest request
	) {
		StackPollutant stackPollutant = stackPollutantService.createStackPollutant(mapper.toCreateCommand(request));
		return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(stackPollutant)));
	}

	@Operation(summary = "시설별 측정물질 목록 조회", description = "stackId 쿼리 파라미터로 시설을 지정합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<List<StackPollutantTableListResponse>>> getStackPollutantList(
		@RequestParam Long stackId
	) {
		return ResponseEntity.ok(ApiResponse.success(
			mapper.toListResponses(stackPollutantService.getStackPollutantList(stackId))
		));
	}

	@Operation(summary = "시설별 측정물질 삭제")
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> removeStackPollutant(@PathVariable Long id) {
		stackPollutantService.removeStackPollutant(id);
		return ResponseEntity.ok(ApiResponse.success());
	}
}
