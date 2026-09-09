package com.ensolution.ems.auth.presentation.controller;

import com.ensolution.ems.auth.application.service.UserService;
import com.ensolution.ems.auth.presentation.mapper.UserMapper;
import com.ensolution.ems.auth.presentation.response.UserListResponse;
import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 인증된 사용자면 누구나 부를 수 있는 사용자 조회. 측정계획 등록의 담당자 선택처럼
 * <b>선택지가 필요한 화면</b>이 소비한다.
 * <p>
 * <b>{@code /api/admin/members}와 목적이 다르다.</b> 그쪽은 ADMIN 전용 회원 관리(생성·수정·삭제)라
 * 로그인 아이디·이메일·연락처를 함께 내리고, 이 엔드포인트는 FIELD·LAB 같은 일반 역할도 부르므로
 * 이름·부서·역할만 내린다. 같은 원장을 보지만 노출 범위가 달라 경로를 합치지 않는다.
 * <p>
 * tenant 범위는 {@code principal.getTenantId()}로만 정한다({@code UserService.getUserList}가
 * WHERE 절에 싣는다). 조회 대상을 요청 파라미터로 받지 않는다.
 */
@Tag(name = "User", description = "사용자 조회 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final UserMapper userMapper;

	@Operation(summary = "사용자 목록 조회", description = "로그인한 사용자가 속한 테넌트의 사용자 목록입니다.")
	@GetMapping()
	public ResponseEntity<ApiResponse<List<UserListResponse>>> getUserList(
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(
			userMapper.toListResponses(userService.getUserList(principal.getTenantId()))
		));
	}
}
