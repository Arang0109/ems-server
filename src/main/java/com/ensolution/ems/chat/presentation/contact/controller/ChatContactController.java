package com.ensolution.ems.chat.presentation.contact.controller;

import com.ensolution.ems.chat.application.service.ChatPresenceService;
import com.ensolution.ems.chat.presentation.contact.mapper.ChatContactMapper;
import com.ensolution.ems.chat.presentation.contact.response.ChatContactResponse;
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
 * 대화 상대 목록.
 * <p>
 * {@code /api/admin/members} 를 재사용하지 않습니다. 그쪽은 ADMIN 전용이라 일반 사용자가 부를 수 없고
 * {@code email}·{@code tel} 까지 내보냅니다. 어떤 필드를 노출할지와 접속 상태를 합치는 일은
 * 채팅 화면의 관심사이므로 이 모듈이 자기 엔드포인트를 갖습니다.
 */
@Tag(name = "Chat Contact", description = "대화 상대 목록 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/chat/contacts")
@RequiredArgsConstructor
public class ChatContactController {

	private final ChatPresenceService chatPresenceService;
	private final ChatContactMapper mapper;

	@Operation(summary = "대화 상대 목록", description = "같은 고객사 구성원입니다. 자기 자신은 빠집니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<List<ChatContactResponse>>> getContacts(
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok(ApiResponse.success(mapper.toResponses(
			chatPresenceService.getContacts(principal.getUserId(), principal.getTenantId())
		)));
	}
}
