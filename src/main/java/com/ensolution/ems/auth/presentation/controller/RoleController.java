package com.ensolution.ems.auth.presentation.controller;

import com.ensolution.ems.auth.application.service.RoleService;
import com.ensolution.ems.auth.presentation.mapper.RoleMapper;
import com.ensolution.ems.auth.presentation.response.RoleResponse;
import com.ensolution.ems.global.web.ApiResponse;
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

	@GetMapping()
	public ResponseEntity<ApiResponse<List<RoleResponse>>> getRoleList() {

		return ResponseEntity.ok().body(ApiResponse.success(
			roleMapper.toResponses(roleService.getRoleList())
		));
	}
}
