package com.ensolution.ems.chat.presentation.room.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 읽음 보고.
 * <p>
 * <b>형식을 여기서 먼저 막습니다.</b> 이 값은 저장되고 끝나는 것이 아니라 다음 미읽음 집계의
 * 쿼리 인자가 되므로, 잘못된 값이 들어가면 그 사용자의 방 목록이 조회될 때마다 실패합니다.
 * 도메인({@code ChatParticipant.readUpTo})도 같은 검사를 하지만, 여기서 걸러야 400과
 * 어느 필드가 잘못됐는지가 함께 나갑니다.
 * <p>
 * 가장 흔한 실수는 {@code clientMessageId}(클라이언트가 만든 UUID)를 보내는 것입니다.
 * 그것은 임시 말풍선을 치환하는 키일 뿐 서버의 메시지 id가 아닙니다.
 */
public record MarkAsReadRequest(
	@Schema(description = "여기까지 읽었다는 메시지 id(24자 16진). clientMessageId 가 아닙니다",
		example = "665f0000000000000000000a")
	@NotBlank(message = "읽은 위치를 지정해 주세요.")
	@Pattern(regexp = "[0-9a-fA-F]{24}", message = "메시지 id 형식이 올바르지 않습니다.")
	String lastReadMessageId
) {
}
