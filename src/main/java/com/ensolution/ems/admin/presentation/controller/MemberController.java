package com.ensolution.ems.admin.presentation.controller;

import com.ensolution.ems.admin.presentation.mapper.MemberMapper;
import com.ensolution.ems.admin.presentation.request.CreateMemberRequest;
import com.ensolution.ems.admin.presentation.request.UpdateMemberRequest;
import com.ensolution.ems.admin.presentation.response.MemberResponse;
import com.ensolution.ems.auth.application.port.in.UserCommandUseCase;
import com.ensolution.ems.auth.application.port.in.UserQueryUseCase;
import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 테넌트 관리자의 회원 관리. 원장은 auth의 {@code User}이며 이 컨트롤러는 그 포트에 위임한다.
 * <p>
 * <b>서비스를 두지 않은 것은 조율할 것이 없기 때문이다.</b> 역할 부여 제한 같은 규칙은 원장 모듈
 * (auth의 {@code UserValidator})이 소유한다. 여기에 규칙을 옮겨 오면 auth의 다른 호출 경로가
 * 그 검증을 우회하게 된다. 문서 관리({@code DocumentManagementController})와 같은 방식이며,
 * admin 고유의 조율 규칙이 생기면 그때 서비스로 승격한다.
 * <p>
 * <b>전 경로가 {@code principal.getTenantId()}를 Command에 실어 보낸다.</b> 이 모듈에는 자체 WHERE 절이
 * 없으므로 tenantId를 빠뜨리면 그 자리에서 교차 테넌트가 된다. 이 파라미터를 지우지 말 것.
 */
@Tag(name = "Member", description = "회원 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class MemberController {

	private final UserCommandUseCase userCommandUseCase;
	private final UserQueryUseCase userQueryUseCase;

	private final MemberMapper memberMapper;

	@Operation(summary = "회원 등록")
	@PostMapping()
	public ResponseEntity<ApiResponse<Void>> createMember(
		@Valid @RequestBody CreateMemberRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		userCommandUseCase.createUser(memberMapper.toCreateCommand(request, principal.getTenantId()));
		return ResponseEntity.ok().body(ApiResponse.success());
	}

	@Operation(summary = "회원 목록 조회")
	@GetMapping()
	public ResponseEntity<ApiResponse<List<MemberResponse>>> getMemberList(
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(
			memberMapper.toResponses(userQueryUseCase.getUserList(principal.getTenantId()))
		));
	}

	@Operation(summary = "회원 단건 조회")
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<MemberResponse>> getMember(
		@PathVariable Long id,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(
			memberMapper.toResponse(userQueryUseCase.getUser(id, principal.getTenantId()))
		));
	}

	@Operation(summary = "회원 수정")
	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> updateMember(
		@PathVariable Long id,
		@Valid @RequestBody UpdateMemberRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		userCommandUseCase.updateUser(memberMapper.toUpdateCommand(request, id, principal.getTenantId()));
		return ResponseEntity.ok().body(ApiResponse.success());
	}

	@Operation(summary = "회원 삭제")
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteMember(
		@PathVariable Long id,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		userCommandUseCase.deleteUser(id, principal.getTenantId());
		return ResponseEntity.ok().body(ApiResponse.success());
	}
}
