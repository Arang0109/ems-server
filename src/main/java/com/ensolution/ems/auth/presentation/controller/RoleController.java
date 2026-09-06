package com.ensolution.ems.auth.presentation.controller;

import com.ensolution.ems.auth.application.service.RoleService;
import com.ensolution.ems.auth.presentation.mapper.RoleMapper;
import com.ensolution.ems.auth.presentation.response.RoleResponse;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Role", description = "권한 관련 API")
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

	private final RoleService roleService;
	private final RoleMapper roleMapper;

	/**
	 * 회원에게 <b>부여할 수 있는</b> 역할 목록. {@code PLATFORM_ADMIN}은 포함되지 않는다 —
	 * 부여가 거부되는 역할({@code ROLE_NOT_ASSIGNABLE})을 선택지로 내려보내지 않기 위해서다.
	 */
	@Operation(summary = "부여 가능한 역할 목록", description = "PLATFORM_ADMIN은 제외됩니다.")
	@GetMapping()
	public ResponseEntity<ApiResponse<List<RoleResponse>>> getAssignableRoles() {

		return ResponseEntity.ok().body(ApiResponse.success(
			roleMapper.toResponses(roleService.getAssignableRoles())
		));
	}
}
