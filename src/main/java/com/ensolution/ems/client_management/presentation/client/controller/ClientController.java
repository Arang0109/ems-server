package com.ensolution.ems.client_management.presentation.client.controller;

import com.ensolution.ems.client_management.application.service.ClientService;
import com.ensolution.ems.client_management.domain.Client;
import com.ensolution.ems.client_management.presentation.client.mapper.ClientMapper;
import com.ensolution.ems.client_management.presentation.client.response.ClientResponse;
import com.ensolution.ems.client_management.presentation.client.request.CreateClientRequest;
import com.ensolution.ems.client_management.presentation.client.request.UpdateClientRequest;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
		@Valid @RequestBody CreateClientRequest request
	) {
		Client savedClient = clientService.createClient(mapper.toCreateCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(savedClient)));
	}

	@Operation(summary = "의뢰기관 목록 조회")
	@GetMapping
	public ResponseEntity<ApiResponse<List<ClientResponse>>> getClientList() {
		List<Client> clients = clientService.getClientList();
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponses(clients)));
	}

	@Operation(summary = "의뢰기관 상세 조회")
	@GetMapping("/{clientId}")
	public ResponseEntity<ApiResponse<ClientResponse>> getClient(
		@PathVariable Long clientId
	) {
		Client client = clientService.getClient(clientId);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(client)));
	}

	@Operation(summary = "의뢰기관 수정", description = "전달하지 않은 필드는 기존 값을 유지합니다.")
	@PutMapping("/{clientId}")
	public ResponseEntity<ApiResponse<ClientResponse>> updateClient(
		@PathVariable Long clientId,
		@RequestBody UpdateClientRequest request
		) {
		Client modifiedClient = clientService.updateClient(clientId, mapper.toUpdateCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(modifiedClient)));
	}

	@Operation(summary = "의뢰기관 삭제")
	@DeleteMapping("/{clientId}")
	public ResponseEntity<ApiResponse<Void>> deleteClient(@PathVariable Long clientId) {
		clientService.deleteClient(clientId);
		return ResponseEntity.ok().body(ApiResponse.success());
	}
}
