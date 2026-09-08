package com.ensolution.ems.chat.presentation.message.controller;

import com.ensolution.ems.chat.application.service.ChatMessageService;
import com.ensolution.ems.chat.application.service.ChatRoomService;
import com.ensolution.ems.chat.presentation.message.mapper.ChatMessageMapper;
import com.ensolution.ems.chat.presentation.message.request.SendMessageRequest;
import com.ensolution.ems.chat.presentation.message.response.ChatMessagePageResponse;
import com.ensolution.ems.chat.presentation.message.response.ChatMessageResponse;
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

/**
 * 메시지 API.
 * <p>
 * <b>전송은 REST 입니다. WebSocket 은 수신 전용입니다.</b> 첨부가 multipart 라 어차피 REST 경로가
 * 필요하고, {@code ApiResponse} 봉투·{@code GlobalExceptionHandler}·{@code @Valid}·
 * {@code @AuthenticationPrincipal}이 전부 서블릿 MVC 에만 있기 때문입니다. STOMP {@code SEND}로
 * 받았다면 응답 봉투·에러 프레임 규약·인가 코드를 전부 새로 만들어야 했습니다.
 * <p>
 * 전송 지연이 화면에 드러나지 않도록 {@code clientMessageId}를 그대로 되돌려 줍니다 —
 * 클라이언트는 응답을 기다리는 동안 임시 말풍선을 띄우고 이 키로 치환합니다.
 */
@Tag(name = "Chat Message", description = "대화 메시지 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatMessageController {

	private final ChatMessageService chatMessageService;
	private final ChatRoomService chatRoomService;
	private final ChatMessageMapper mapper;

	@Operation(summary = "메시지 전송")
	@PostMapping("/rooms/{roomId}/messages")
	public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
		@PathVariable Long roomId,
		@Valid @RequestBody SendMessageRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(
			chatMessageService.sendMessage(
				mapper.toSendCommand(request, roomId, principal.getUserId(), principal.getTenantId()))
		)));
	}

	@Operation(summary = "대화 이력 조회",
		description = "최신순입니다. 위로 더 읽으려면 응답의 nextCursor 를 before 로 넘깁니다. "
			+ "offset 이 아니라 커서를 쓰는 이유는 읽는 동안 앞에 새 메시지가 붙어도 경계가 밀리지 않게 하기 위함입니다.")
	@GetMapping("/rooms/{roomId}/messages")
	public ResponseEntity<ApiResponse<ChatMessagePageResponse>> getMessages(
		@PathVariable Long roomId,
		@RequestParam(required = false) String before,
		@RequestParam(required = false) Integer size,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok(ApiResponse.success(mapper.toPageResponse(
			chatMessageService.getMessages(roomId, principal.getUserId(), principal.getTenantId(), before, size)
		)));
	}

	@Operation(summary = "전체 안 읽은 메시지 수", description = "화면 상단 배지에 씁니다. 감춰 둔 방은 세지 않습니다.")
	@GetMapping("/unread-count")
	public ResponseEntity<ApiResponse<Long>> getUnreadCount(
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok(ApiResponse.success(
			chatRoomService.getTotalUnreadCount(principal.getUserId(), principal.getTenantId())
		));
	}
}
