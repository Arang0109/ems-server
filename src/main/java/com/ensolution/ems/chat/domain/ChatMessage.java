package com.ensolution.ems.chat.domain;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 대화 한 건. <b>한 번 기록되면 바뀌지 않으므로 {@code update()}를 두지 않습니다</b>
 * ({@code storage}의 {@code DocumentVersion}, {@code equipment}의 {@code InspectionRecord}와 같은 성격).
 * <p>
 * {@code id}는 저장소가 부여합니다(MongoDB {@code ObjectId}의 16진 문자열). 이 값이
 * <b>정렬 축이자 커서</b>입니다 — ObjectId는 앞 4바이트가 초 단위 타임스탬프이고 뒤이어
 * 단조 증가 카운터가 오므로 문자열 사전순이 곧 생성순입니다. {@code sentAt}을 커서로 쓰지 않는
 * 이유는 같은 밀리초에 두 건이 들어올 수 있어 경계에서 메시지가 누락되거나 중복되기 때문입니다.
 * <p>
 * {@code clientMessageId}는 클라이언트가 만든 UUID입니다. 전송은 REST라 응답을 기다리는 동안
 * 화면에 임시 말풍선을 띄우는데, 서버가 이 값을 응답과 브로드캐스트 양쪽에 그대로 돌려주므로
 * 클라이언트가 임시 항목을 실제 메시지로 치환할 수 있습니다. 서버는 해석하지 않습니다.
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ChatMessage {

	private String id;
	private Long tenantId;
	private Long roomId;
	private Long senderId;
	private ChatMessageType type;
	private String content;
	private String clientMessageId;
	private LocalDateTime sentAt;

	public static ChatMessage text(Long tenantId, Long roomId, Long senderId, String content,
		String clientMessageId) {
		requireContent(content);

		return ChatMessage.builder()
			.tenantId(tenantId)
			.roomId(roomId)
			.senderId(senderId)
			.type(ChatMessageType.TEXT)
			.content(content)
			.clientMessageId(clientMessageId)
			.build();
	}

	/**
	 * 목록에 보여 줄 한 줄 요약. 첨부만 있는 메시지는 본문이 비어 있으므로 파일임을 알린다.
	 * 자르기는 {@code ChatRoom}이 컬럼 길이에 맞춰 한다.
	 */
	public String preview() {
		return switch (type) {
			case TEXT -> content;
			case IMAGE -> "사진";
			case FILE -> "파일";
		};
	}

	private static void requireContent(String content) {
		if (content == null || content.isBlank()) {
			throw new CustomException(ErrorCode.CHAT_MESSAGE_EMPTY);
		}
	}
}
