package com.ensolution.ems.chat.presentation.room.controller;

import com.ensolution.ems.chat.application.service.ChatRoomService;
import com.ensolution.ems.chat.presentation.room.mapper.ChatRoomMapper;
import com.ensolution.ems.chat.presentation.message.mapper.ChatMessageMapper;
import com.ensolution.ems.chat.presentation.room.request.MarkAsReadRequest;
import com.ensolution.ems.chat.presentation.room.request.OpenDirectRoomRequest;
import com.ensolution.ems.chat.presentation.room.response.ChatRoomListResponse;
import com.ensolution.ems.chat.presentation.room.response.ChatRoomResponse;
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

/**
 * 1:1 대화방 API.
 * <p>
 * 역할 제한을 두지 않습니다 — 채팅은 테넌트 내부 전원의 기능이고, 격리는 참가자 검증과
 * tenant 범위 조회가 담당합니다. {@code userId}·{@code tenantId}는 언제나
 * {@code @AuthenticationPrincipal}에서만 얻습니다(루트 규칙 13).
 */
@Tag(name = "Chat Room", description = "1:1 대화방 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

	private final ChatRoomService chatRoomService;
	private final ChatRoomMapper mapper;
	/** 읽음 커맨드 변환은 메시지 매퍼가 갖는다 — MarkAsReadCommand 가 메시지 id 를 다루기 때문이다. */
	private final ChatMessageMapper messageMapper;

	@Operation(summary = "1:1 대화방 열기",
		description = "이미 두 사람의 대화방이 있으면 새로 만들지 않고 그 방을 반환합니다(멱등).")
	@PostMapping
	public ResponseEntity<ApiResponse<ChatRoomResponse>> openDirectRoom(
		@Valid @RequestBody OpenDirectRoomRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(
			chatRoomService.openDirectRoom(
				mapper.toOpenCommand(request, principal.getUserId(), principal.getTenantId()))
		)));
	}

	@Operation(summary = "내 대화방 목록", description = "최근 대화 순입니다. 나가기로 감춘 방은 빠집니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<List<ChatRoomListResponse>>> getRoomList(
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok(ApiResponse.success(mapper.toListResponses(
			chatRoomService.getRoomList(principal.getUserId(), principal.getTenantId())
		)));
	}

	@Operation(summary = "대화방 단건")
	@GetMapping("/{roomId}")
	public ResponseEntity<ApiResponse<ChatRoomResponse>> getRoom(
		@PathVariable Long roomId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(
			chatRoomService.getRoom(roomId, principal.getUserId(), principal.getTenantId())
		)));
	}

	@Operation(summary = "읽음 처리",
		description = "여기까지 읽었다고 보고합니다. 커서는 앞으로만 가므로 이전 위치를 보내면 무시됩니다.")
	@PostMapping("/{roomId}/read")
	public ResponseEntity<ApiResponse<Void>> markAsRead(
		@PathVariable Long roomId,
		@Valid @RequestBody MarkAsReadRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		chatRoomService.markAsRead(messageMapper.toMarkAsReadCommand(
			request, roomId, principal.getUserId(), principal.getTenantId()));
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "대화방 나가기",
		description = "내 목록에서만 감춥니다. 상대의 대화 기록은 남고, 상대가 새 메시지를 보내면 다시 나타납니다.")
	@DeleteMapping("/{roomId}")
	public ResponseEntity<ApiResponse<Void>> hideRoom(
		@PathVariable Long roomId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		chatRoomService.hideRoom(roomId, principal.getUserId(), principal.getTenantId());
		return ResponseEntity.ok(ApiResponse.success());
	}
}
