package com.ensolution.ems.client_management.presentation;

import com.ensolution.ems.client_management.application.WorkplaceService;
import com.ensolution.ems.client_management.domain.Workplace;
import com.ensolution.ems.client_management.presentation.mapper.WorkplacePresentationMapper;
import com.ensolution.ems.client_management.presentation.request.CreateWorkplaceRequest;
import com.ensolution.ems.client_management.presentation.request.UpdateWorkplaceRequest;
import com.ensolution.ems.client_management.presentation.response.WorkplaceResponse;
import com.ensolution.ems.client_management.presentation.response.WorkplaceListResponse;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Workplace", description = "측정대상 사업장 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/workplaces")
@RequiredArgsConstructor
public class WorkplaceController {
	
	private final WorkplaceService workplaceService;
	private final WorkplacePresentationMapper mapper;
	
	@PostMapping()
	public ResponseEntity<ApiResponse<WorkplaceResponse>> createWorkplace(
		@Valid @RequestBody CreateWorkplaceRequest request
	) {
		Workplace workplace = workplaceService.createWorkplace(mapper.toCreateCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(workplace)));
	}
	
	@GetMapping()
	public ResponseEntity<ApiResponse<List<WorkplaceListResponse>>> getWorkplaceList(
		@RequestParam Long companyId
	) {
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toListResponses(
			workplaceService.getWorkplaceList(companyId)
		)));
	}
	
	@GetMapping("/{workplaceId}")
	public ResponseEntity<ApiResponse<WorkplaceResponse>> getWorkplaceDetail(@PathVariable Long workplaceId) {
		Workplace workplace = workplaceService.getWorkplace(workplaceId);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(workplace)));
	}
	
	@PutMapping("/{workplaceId}")
	public ResponseEntity<ApiResponse<WorkplaceResponse>> updateWorkplace(
		@PathVariable Long workplaceId,
		@RequestBody UpdateWorkplaceRequest request
	) {
		Workplace workplace = workplaceService.updateWorkplace(workplaceId, mapper.toUpdateCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(workplace)));
	}

	@DeleteMapping("/{workplaceId}")
	public ResponseEntity<ApiResponse<Void>> deleteWorkplace(@PathVariable Long workplaceId) {
		workplaceService.deleteWorkplace(workplaceId);
		return ResponseEntity.ok().body(ApiResponse.success());
	}
}
