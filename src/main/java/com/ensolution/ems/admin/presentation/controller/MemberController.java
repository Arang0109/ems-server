package com.ensolution.ems.admin.presentation.controller;

import com.ensolution.ems.admin.application.service.MemberService;
import com.ensolution.ems.admin.presentation.mapper.MemberMapper;
import com.ensolution.ems.admin.presentation.request.CreateMemberRequest;
import com.ensolution.ems.admin.presentation.request.UpdateMemberRequest;
import com.ensolution.ems.admin.presentation.response.MemberResponse;
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

@Tag(name = "Member", description = "회원 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class MemberController {
	
	private final MemberService memberService;
	
	private final MemberMapper memberMapper;
	
	@Operation(summary = "회원 등록")
	@PostMapping()
	public ResponseEntity<ApiResponse<MemberResponse>> createMember(
		@Valid @RequestBody CreateMemberRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		memberService.createMember(memberMapper.toCreateMemberCommand(request, principal.getTenantId()));
		return ResponseEntity.ok().body(ApiResponse.success());
	}
	
	@Operation(summary = "회원 목록 조회")
	@GetMapping()
	public ResponseEntity<ApiResponse<List<MemberResponse>>> getMemberList(
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(
			memberMapper.toResponses(memberService.getMemberList(principal.getTenantId()))
		));
	}

	@Operation(summary = "회원 단건 조회")
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<MemberResponse>> getMember(
		@PathVariable Long id
	) {
		return ResponseEntity.ok().body(ApiResponse.success(
			memberMapper.toResponse(memberService.getMember(id))
		));
	}

	@Operation(summary = "회원 수정")
	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<MemberResponse>> updateMember(
		@PathVariable Long id,
		@Valid @RequestBody UpdateMemberRequest request
	) {
		memberService.updateMember(memberMapper.toUpdateMemberCommand(request, id));
		return ResponseEntity.ok().body(ApiResponse.success());
	}

	@Operation(summary = "회원 삭제")
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteMember(
		@PathVariable Long id
	) {
		memberService.deleteMember(id);
		return ResponseEntity.ok().body(ApiResponse.success());
	}
}
