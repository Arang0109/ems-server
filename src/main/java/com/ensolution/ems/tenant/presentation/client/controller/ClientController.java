package com.ensolution.ems.tenant.presentation.client.controller;

import com.ensolution.ems.tenant.application.service.ClientService;
import com.ensolution.ems.tenant.domain.Client;
import com.ensolution.ems.tenant.presentation.client.mapper.ClientMapper;
import com.ensolution.ems.tenant.presentation.client.response.ClientResponse;
import com.ensolution.ems.tenant.presentation.client.request.CreateClientRequest;
import com.ensolution.ems.tenant.presentation.client.request.UpdateClientRequest;
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

@Tag(name = "Client", description = "측정대행 의뢰기관 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

	private final ClientService clientService;
	private final ClientMapper mapper;

	@Operation(summary = "의뢰기관 등록")
	@PostMapping
	public ResponseEntity<ApiResponse<ClientResponse>> createClient(
		@Valid @RequestBody CreateClientRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		Client savedClient = clientService.createClient(mapper.toCreateCommand(request, principal.getTenantId()));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(savedClient)));
	}

	@Operation(summary = "의뢰기관 목록 조회")
	@GetMapping
	public ResponseEntity<ApiResponse<List<ClientResponse>>> getClientList(
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		List<Client> clients = clientService.getClientList(principal.getTenantId());
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponses(clients)));
	}

	@Operation(summary = "의뢰기관 상세 조회")
	@GetMapping("/{clientId}")
	public ResponseEntity<ApiResponse<ClientResponse>> getClient(
		@PathVariable Long clientId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		Client client = clientService.getClient(clientId, principal.getTenantId());
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(client)));
	}

	@Operation(summary = "의뢰기관 수정", description = "전달하지 않은 필드는 기존 값을 유지합니다.")
	@PutMapping("/{clientId}")
	public ResponseEntity<ApiResponse<ClientResponse>> updateClient(
		@PathVariable Long clientId,
		@RequestBody UpdateClientRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
		) {
		Client modifiedClient = clientService.updateClient(clientId, principal.getTenantId(), mapper.toUpdateCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(modifiedClient)));
	}

	@Operation(summary = "의뢰기관 삭제")
	@DeleteMapping("/{clientId}")
	public ResponseEntity<ApiResponse<Void>> deleteClient(
		@PathVariable Long clientId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		clientService.deleteClient(clientId, principal.getTenantId());
		return ResponseEntity.ok().body(ApiResponse.success());
	}
}
