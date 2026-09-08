package com.ensolution.ems.chat.domain;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

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

	/**
	 * 메시지 id 의 형태 — MongoDB {@code ObjectId} 의 16진 표현.
	 * <p>
	 * <b>id 형식을 메시지가 소유하는 이유</b>: 이 값은 저장만 되는 것이 아니라 <b>다음 조회의 쿼리 인자</b>가
	 * 된다. 읽음 커서로 잘못된 문자열이 한 번 저장되면 그 뒤의 모든 미읽음 집계가 터진다
	 * ({@code new ObjectId(...)} 가 던진다). 그래서 저장 경로가 아니라 <b>값의 주인</b>이 형태를 지킨다.
	 */
	private static final Pattern ID_FORMAT = Pattern.compile("[0-9a-fA-F]{24}");

	private String id;
	private Long tenantId;
	private Long roomId;
	private Long senderId;
	private ChatMessageType type;
	private String content;

	/** 첨부가 없으면 null 이다. 실물은 보관소에 있고 여기에는 키와 표시 정보만 있다 */
	private ChatAttachment attachment;

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

	/**
	 * 첨부가 있는 메시지. 본문은 캡션이라 없어도 된다 — 그래서 {@link #text}와 달리 내용 검사를 하지 않는다.
	 * 종류는 사용자가 고르지 않고 {@code contentType}이 정한다.
	 */
	public static ChatMessage withAttachment(Long tenantId, Long roomId, Long senderId, String content,
		ChatAttachment attachment, String clientMessageId) {
		return ChatMessage.builder()
			.tenantId(tenantId)
			.roomId(roomId)
			.senderId(senderId)
			.type(attachment.messageType())
			.content(content)
			.attachment(attachment)
			.clientMessageId(clientMessageId)
			.build();
	}

	/**
	 * 첨부의 실물을 내려보낼 수 있는 메시지인지 확인한다.
	 * 첨부 없는 메시지의 다운로드 요청은 잘못된 요청이지 서버 오류가 아니다.
	 */
	public ChatAttachment requireAttachment() {
		if (attachment == null) {
			throw new CustomException(ErrorCode.CHAT_ATTACHMENT_NOT_FOUND);
		}
		return attachment;
	}

	/** 커서·참조로 오가는 메시지 id 가 쿼리에 넣어도 되는 형태인지. */
	public static boolean isValidId(String messageId) {
		return messageId != null && ID_FORMAT.matcher(messageId).matches();
	}

	/**
	 * 형태가 아니면 저장 전에 막는다. 잘못된 값이 들어가면 되돌리기 어렵다 —
	 * 임의 문자열은 실제 ObjectId(6·7 로 시작)보다 사전순으로 큰 경우가 많아
	 * 앞으로만 가는 커서가 <b>다시는 덮어쓰지 못한다.</b>
	 */
	public static String requireValidId(String messageId) {
		if (!isValidId(messageId)) {
			throw new CustomException(ErrorCode.CHAT_INVALID_MESSAGE_ID);
		}
		return messageId;
	}

	private static void requireContent(String content) {
		if (content == null || content.isBlank()) {
			throw new CustomException(ErrorCode.CHAT_MESSAGE_EMPTY);
		}
	}
}
