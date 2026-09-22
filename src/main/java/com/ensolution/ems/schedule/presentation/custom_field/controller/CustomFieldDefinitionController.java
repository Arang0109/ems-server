package com.ensolution.ems.schedule.presentation.custom_field.controller;

import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.web.ApiResponse;
import com.ensolution.ems.schedule.application.service.CustomFieldDefinitionService;
import com.ensolution.ems.schedule.presentation.custom_field.mapper.CustomFieldDefinitionMapper;
import com.ensolution.ems.schedule.presentation.custom_field.request.CreateCustomFieldDefinitionRequest;
import com.ensolution.ems.schedule.presentation.custom_field.request.UpdateCustomFieldDefinitionRequest;
import com.ensolution.ems.schedule.presentation.custom_field.response.CustomFieldDefinitionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 측정계획 커스텀 필드 정의 API.
 *
 * <p>경로가 {@code /api/admin/**}이 아니라 {@code /api/schedules/custom-fields}인 이유 — 정의는 키 형식·중복 같은
 * 규칙을 가진 애그리거트라 원장 없는 {@code admin} 모듈에 둘 수 없고, admin 경로로 노출하려면 schedule이
 * {@code port/in}을 새로 열어야 한다. 대신 쓰기 3경로에 {@code @PreAuthorize("hasRole('ADMIN')")}를 건다
 * ({@code PlatformPollutantCatalogController} 선례). 목록 조회는 회차 값 입력 폼이 라벨을 그려야 하므로
 * 인증 사용자 전체에 연다.
 *
 * <p>{@code /api/schedules/{scheduleId}}와 같은 prefix를 쓰지만 {@code custom-fields}가 리터럴이라 더 구체적인
 * 패턴으로 먼저 매칭된다({@code /api/schedules/canceled}와 같은 방식). 회귀는 {@code ScheduleRoutingTest}가 고정한다.
 */
@Tag(name = "ScheduleCustomField", description = "측정계획 커스텀 필드 정의 API — 성적서 템플릿이 ${custom.<key>}로 읽는 이름을 고객사가 관리합니다")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/schedules/custom-fields")
@RequiredArgsConstructor
public class CustomFieldDefinitionController {

	private final CustomFieldDefinitionService customFieldDefinitionService;
	private final CustomFieldDefinitionMapper mapper;

	@Operation(
		summary = "커스텀 필드 등록 (ADMIN)",
		description = """
			템플릿이 `${custom.<key>}`로 읽을 이름을 만듭니다. `key`는 영문자·숫자·밑줄만 쓸 수 있고 영문자 또는
			밑줄로 시작해야 하며, **등록 후 바꿀 수 없습니다** — 배포된 템플릿과 저장된 값의 계약이기 때문입니다.
			이름을 바꾸려면 삭제 후 다시 등록합니다. `sortOrder`를 비우면 목록 맨 뒤에 붙습니다.
			""")
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping
	public ResponseEntity<ApiResponse<CustomFieldDefinitionResponse>> createDefinition(
		@Valid @RequestBody CreateCustomFieldDefinitionRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		CustomFieldDefinitionResponse response = mapper.toResponse(
			customFieldDefinitionService.createDefinition(mapper.toCreateCommand(request, principal.getTenantId()))
		);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "커스텀 필드 목록 조회", description = "표시 순서(`sortOrder`)대로 돌려줍니다. 회차 값 입력 폼이 이 목록으로 칸을 그립니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<List<CustomFieldDefinitionResponse>>> getDefinitionList(
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok(ApiResponse.success(
			mapper.toResponses(customFieldDefinitionService.getDefinitionList(principal.getTenantId()))
		));
	}

	@Operation(summary = "커스텀 필드 수정 (ADMIN)", description = "라벨·표시 순서만 고칩니다. 비운 필드는 기존 값이 유지되고, 키는 바꿀 수 없습니다.")
	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/{fieldId}")
	public ResponseEntity<ApiResponse<CustomFieldDefinitionResponse>> updateDefinition(
		@PathVariable Long fieldId,
		@Valid @RequestBody UpdateCustomFieldDefinitionRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		CustomFieldDefinitionResponse response = mapper.toResponse(
			customFieldDefinitionService.updateDefinition(fieldId, principal.getTenantId(), mapper.toUpdateCommand(request))
		);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(
		summary = "커스텀 필드 삭제 (ADMIN)",
		description = """
			정의만 지웁니다. 이미 회차에 저장된 값은 측정 시점 사본이라 그대로 남고, 템플릿이 그 키를 계속 참조하면
			계속 출력됩니다. 그 회차의 커스텀 필드를 다음에 저장할 때 정리됩니다.
			""")
	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/{fieldId}")
	public ResponseEntity<ApiResponse<Void>> deleteDefinition(
		@PathVariable Long fieldId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		customFieldDefinitionService.deleteDefinition(fieldId, principal.getTenantId());
		return ResponseEntity.ok(ApiResponse.success());
	}
}
