package com.ensolution.ems.schedule.presentation.controller;

import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.schedule.application.service.ScheduleStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 측정계획 편집 이벤트 스트림. 여러 사용자가 같은 기록지를 나눠 입력할 때, 저장 즉시
 * 나머지 사용자의 화면이 최신 값을 반영하도록 알린다.
 * <p>
 * 이 엔드포인트만 {@code ApiResponse} 봉투를 쓰지 않는다 — 본문이 단일 JSON이 아니라
 * 연결이 유지되는 이벤트 스트림이기 때문이다.
 */
@Tag(name = "Schedule Stream", description = "측정계획 편집 실시간 알림 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleStreamController {

	/**
	 * 연결 수명(ms). 만료되면 클라이언트가 다시 붙는다 — 무한(0)으로 두면 죽은 연결이
	 * 서버에 계속 쌓이므로 넉넉하되 유한하게 잡는다.
	 */
	private static final long STREAM_TIMEOUT_MS = 30 * 60 * 1000L;

	private final ScheduleStreamService scheduleStreamService;

	@Operation(summary = "측정계획 편집 이벤트 구독",
		description = "같은 측정계획을 열어둔 다른 사용자가 측정 시트를 저장하면 sheets-saved 이벤트를 보냅니다. "
			+ "이벤트에는 시트 본문이 아니라 바뀐 카테고리와 저장한 사용자 id만 담기므로, "
			+ "수신 측은 GET /api/schedules/{scheduleId} 로 최신본을 조회해 반영합니다.")
	@GetMapping(value = "/{scheduleId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public ResponseEntity<SseEmitter> subscribe(
		@PathVariable Long scheduleId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
		scheduleStreamService.subscribe(scheduleId, principal.getTenantId(), emitter);

		// nginx는 프록시 응답을 기본으로 버퍼링해 이벤트를 뭉쳐 내보낸다. 이 헤더가 있으면
		// 해당 응답만 버퍼링이 꺼지므로 프록시 설정에 의존하지 않고 실시간성을 지킬 수 있다.
		return ResponseEntity.ok()
			.header("X-Accel-Buffering", "no")
			.body(emitter);
	}
}
