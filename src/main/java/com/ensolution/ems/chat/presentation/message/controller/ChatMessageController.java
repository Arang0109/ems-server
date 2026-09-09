package com.ensolution.ems.chat.presentation.message.controller;

import com.ensolution.ems.chat.application.command.AttachmentUpload;
import com.ensolution.ems.chat.application.command.ChatAttachmentFile;
import com.ensolution.ems.chat.application.service.ChatMessageService;
import com.ensolution.ems.chat.application.service.ChatRoomService;
import com.ensolution.ems.chat.presentation.message.mapper.ChatMessageMapper;

import com.ensolution.ems.chat.presentation.message.request.SendMessageRequest;
import com.ensolution.ems.chat.presentation.message.response.ChatMessagePageResponse;
import com.ensolution.ems.chat.presentation.message.response.ChatMessageResponse;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
@Validated
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

	/**
	 * 첨부 전송.
	 * <p>
	 * <b>텍스트 부분을 JSON 파트가 아니라 폼 파라미터로 받습니다.</b> JSON 파트로 두면 파일만 보내려는
	 * 클라이언트도 빈 파트를 만들어야 하고, 만들지 않으면 400이 아니라 <b>500</b>이 납니다
	 * ({@code MissingServletRequestPartException}이 포괄 핸들러에 잡힙니다). 파트를 보내더라도
	 * {@code Content-Type: application/json}을 파트에 명시하지 않으면 역시 500입니다.
	 * <p>
	 * 파일만 보내는 것이 가장 흔한 사용이므로 그 경로가 가장 단순해야 하고, 이 저장소의 다른 multipart
	 * 엔드포인트({@code admin}의 문서 업로드)도 같은 형태입니다.
	 */
	@Operation(summary = "첨부 전송",
		description = "파일만 보내도 됩니다 — content 는 캡션이라 생략할 수 있습니다. 10MB 를 넘을 수 없습니다.")
	@PostMapping(value = "/rooms/{roomId}/messages/attachments",
		consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<ChatMessageResponse>> sendAttachment(
		@PathVariable Long roomId,
		@RequestParam(required = false) @Size(max = 4000, message = "메시지는 4000자를 넘을 수 없습니다.")
		String content,
		@RequestParam(required = false) @Size(max = 64) String clientMessageId,
		@RequestPart("file") MultipartFile file,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(
			chatMessageService.sendMessage(mapper.toSendCommand(
				content, clientMessageId, toUpload(file),
				roomId, principal.getUserId(), principal.getTenantId()))
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

	/**
	 * 첨부 다운로드. <b>이 엔드포인트만 {@code ApiResponse} 봉투를 쓰지 않습니다</b> —
	 * 본문이 JSON 이 아니라 파일 바이트이기 때문입니다(루트 규칙 6의 예외).
	 * {@code Content-Disposition} 파일명은 한글이 깨지지 않도록 URL 인코딩합니다.
	 */
	@Operation(summary = "첨부 다운로드")
	@GetMapping("/rooms/{roomId}/messages/{messageId}/attachment")
	public ResponseEntity<byte[]> downloadAttachment(
		@PathVariable Long roomId,
		@PathVariable String messageId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		ChatAttachmentFile file = chatMessageService.getAttachment(
			roomId, messageId, principal.getUserId(), principal.getTenantId());

		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(file.filename()))
			.contentType(mediaTypeOf(file.contentType()))
			.body(file.content());
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

	/**
	 * {@code MultipartFile}을 application 페이로드로 바꾼다. 이 변환이 있어야 Spring Web 타입이
	 * application·domain 으로 새지 않는다({@code storage}의 {@code UploadedFile}과 같은 이유).
	 */
	private static AttachmentUpload toUpload(MultipartFile file) {
		try {
			return new AttachmentUpload(
				file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getBytes());
		} catch (IOException e) {
			throw new CustomException(ErrorCode.STORAGE_READ_FAILED, "첨부 파일을 읽지 못했습니다.", e);
		}
	}

	/** 브라우저가 해석하지 못하는 타입은 그냥 내려받게 둔다. */
	private static MediaType mediaTypeOf(String contentType) {
		return contentType == null
			? MediaType.APPLICATION_OCTET_STREAM
			: MediaType.parseMediaType(contentType);
	}

	/** Content-Disposition 헤더값 생성(파일명 UTF-8 인코딩). */
	private static String contentDisposition(String filename) {
		String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
		return "attachment; filename*=UTF-8''" + encoded;
	}
}
