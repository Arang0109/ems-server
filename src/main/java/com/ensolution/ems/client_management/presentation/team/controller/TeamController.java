package com.ensolution.ems.client_management.presentation.team.controller;

import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.web.ApiResponse;
import com.ensolution.ems.client_management.application.service.TeamService;
import com.ensolution.ems.client_management.presentation.team.mapper.TeamMapper;
import com.ensolution.ems.client_management.presentation.team.request.CreateTeamRequest;
import com.ensolution.ems.client_management.presentation.team.request.UpdateTeamRequest;
import com.ensolution.ems.client_management.presentation.team.response.TeamResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Team", description = "측정 팀 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

	private final TeamService teamService;
	private final TeamMapper mapper;

	@Operation(summary = "측정 팀 등록")
	@PostMapping
	public ResponseEntity<ApiResponse<TeamResponse>> createTeam(
		@Valid @RequestBody CreateTeamRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		TeamResponse response = mapper.toResponse(
			teamService.createTeam(mapper.toCreateCommand(request, principal.getTenantId()))
		);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "측정 팀 목록 조회")
	@GetMapping
	public ResponseEntity<ApiResponse<List<TeamResponse>>> getTeamList(
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok(ApiResponse.success(
			mapper.toListResponses(teamService.getTeamList(principal.getTenantId()))
		));
	}

	@Operation(summary = "측정 팀 상세 조회")
	@GetMapping("/{teamId}")
	public ResponseEntity<ApiResponse<TeamResponse>> getTeam(
		@PathVariable Long teamId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok(ApiResponse.success(
			mapper.toResponse(teamService.getTeam(teamId, principal.getTenantId()))
		));
	}

	@Operation(summary = "측정 팀 수정", description = "전달하지 않은 필드는 기존 값을 유지합니다.")
	@PutMapping("/{teamId}")
	public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(
		@PathVariable Long teamId,
		@RequestBody UpdateTeamRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		TeamResponse response = mapper.toResponse(
			teamService.updateTeam(teamId, principal.getTenantId(), mapper.toUpdateCommand(request))
		);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "측정 팀 삭제")
	@DeleteMapping("/{teamId}")
	public ResponseEntity<ApiResponse<Void>> deleteTeam(
		@PathVariable Long teamId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		teamService.deleteTeam(teamId, principal.getTenantId());
		return ResponseEntity.ok(ApiResponse.success());
	}
}
