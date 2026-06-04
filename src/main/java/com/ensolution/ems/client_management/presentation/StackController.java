package com.ensolution.ems.client_management.presentation;

import com.ensolution.ems.client_management.application.StackService;
import com.ensolution.ems.client_management.domain.Stack;
import com.ensolution.ems.client_management.presentation.mapper.StackPresentationMapper;
import com.ensolution.ems.client_management.presentation.request.CreateStackRequest;
import com.ensolution.ems.client_management.presentation.response.StackListResponse;
import com.ensolution.ems.client_management.presentation.response.StackResponse;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Stack", description = "측정시설 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/stacks")
@RequiredArgsConstructor
public class StackController {
	
	private final StackService stackService;
	private final StackPresentationMapper mapper;
	
	@PostMapping()
	public ResponseEntity<ApiResponse<StackResponse>> createStack(
		@Valid @RequestBody CreateStackRequest request
	) {
		Stack stack = stackService.createStack(mapper.toCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(stack)));
	}
	
	@GetMapping()
	public ResponseEntity<ApiResponse<List<StackListResponse>>> getStackList(
		@RequestParam Long workplaceId
	) {
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toListResponses(
				stackService.getStackList(workplaceId)
		)));
	}
}
