package com.ensolution.ems.client_management.presentation.workplace;

import com.ensolution.ems.client_management.application.service.WorkplaceService;
import com.ensolution.ems.client_management.domain.Workplace;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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
	private final WorkplaceMapper mapper;
	
	@Operation(summary = "사업장 등록")
	@PostMapping()
	public ResponseEntity<ApiResponse<WorkplaceResponse>> createWorkplace(
		@Valid @RequestBody CreateWorkplaceRequest request
	) {
		Workplace workplace = workplaceService.createWorkplace(mapper.toCreateCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(workplace)));
	}
	
	@Operation(summary = "사업장 목록 조회", description = "companyId 쿼리 파라미터로 의뢰기관을 지정합니다.")
	@GetMapping()
	public ResponseEntity<ApiResponse<List<WorkplaceTableResponse>>> getWorkplaceList(
		@RequestParam(required = false) Long companyId
	) {
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toListResponses(
			workplaceService.getWorkplaceList(companyId)
		)));
	}
	
	@Operation(summary = "사업장 상세 조회")
	@GetMapping("/{workplaceId}")
	public ResponseEntity<ApiResponse<WorkplaceResponse>> getWorkplaceDetail(@PathVariable Long workplaceId) {
		Workplace workplace = workplaceService.getWorkplace(workplaceId);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(workplace)));
	}
	
	@Operation(summary = "사업장 수정", description = "전달하지 않은 필드는 기존 값을 유지합니다.")
	@PutMapping("/{workplaceId}")
	public ResponseEntity<ApiResponse<WorkplaceResponse>> updateWorkplace(
		@PathVariable Long workplaceId,
		@RequestBody UpdateWorkplaceRequest request
	) {
		Workplace workplace = workplaceService.updateWorkplace(workplaceId, mapper.toUpdateCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(workplace)));
	}

	@Operation(summary = "사업장 삭제")
	@DeleteMapping("/{workplaceId}")
	public ResponseEntity<ApiResponse<Void>> deleteWorkplace(@PathVariable Long workplaceId) {
		workplaceService.deleteWorkplace(workplaceId);
		return ResponseEntity.ok().body(ApiResponse.success());
	}
}
