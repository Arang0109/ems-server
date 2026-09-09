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
 * 테넌트 내 사용자 조회. <b>전체 인증 사용자</b>에게 열린다
 * ({@code /api/users}는 {@code SecurityConfig}의 {@code anyRequest().authenticated()}에 걸린다).
 * <p>
 * <b>ADMIN 전용인 {@code /api/admin/members}와 별개인 이유는 소비자와 필드 범위가 다르기 때문이다.</b>
 * 측정계획 등록처럼 사람을 고르는 화면은 역할과 무관하게 목록이 필요하지만 이름·부서·역할이면 충분하고,
 * 회원 관리 화면은 ADMIN만 보되 로그인 아이디·연락처까지 필요하다. 하나의 엔드포인트로 합치면
 * 둘 중 하나가 과한 권한이거나 과한 노출이 된다. 무엇을 빼는지는 {@link UserListResponse} 참고.
 * <p>
 * <b>여기에 생성·수정·삭제를 더하지 말 것.</b> 계정이 만들어지는 경로는
 * {@code /api/admin/members}(ADMIN)와 {@code platform}의 테넌트 프로비저닝, 부트스트랩 셋뿐이며
 * 공개 가입을 두지 않는다는 규칙(auth 모듈 문서)이 여기에 걸려 있다. 이 컨트롤러는 조회만 갖는다.
 * <p>
 * <b>{@code principal.getTenantId()}를 지우지 말 것.</b> 격리는 이 인자 하나에 달려 있다.
 */
@Tag(name = "User", description = "사용자 조회 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final UserMapper userMapper;

	@Operation(
		summary = "테넌트 사용자 목록 조회",
		description = "선택지 렌더링용입니다. 로그인 아이디·이메일·연락처는 포함되지 않습니다."
	)
	@GetMapping()
	public ResponseEntity<ApiResponse<List<UserListResponse>>> getUserList(
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(
			userMapper.toListResponses(userService.getUserList(principal.getTenantId()))
		));
	}
}
