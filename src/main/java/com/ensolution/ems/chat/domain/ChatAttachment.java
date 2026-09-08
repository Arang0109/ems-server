package com.ensolution.ems.chat.domain;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 메시지에 딸린 파일. 실물은 {@code global/storage}의 보관소에 있고 여기에는 그 키와 표시 정보만 둡니다.
 * <p>
 * <b>{@code storageKey}는 도메인이 만듭니다.</b> 보관소 어댑터는 키를 해석할 뿐 만들지 않습니다
 * ({@code storage}의 {@code DocumentVersion}과 같은 규약). 사용자가 올린 파일명을 경로에 섞으면
 * 경로 순회와 동명 덮어쓰기가 열리므로 UUID로 만들고, 원본 파일명은 내려보낼 때만 씁니다.
 * <p>
 * 키 앞에 {@code chat/}을 두어 문서 보관소와 섞이지 않게 합니다. S3 어댑터가
 * {@code ems.storage.s3.key-prefix}(현재 {@code documents})를 다시 앞에 붙이므로 실제 오브젝트 키는
 * {@code documents/chat/...}이 됩니다 — prefix 는 바꾸면 기존 파일을 찾지 못하므로 그대로 둡니다.
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ChatAttachment {

	/** 전역 multipart 한도(20MB)와 별개로 채팅이 스스로 두는 한도. */
	public static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

	/** 확장자로 인정할 형태. 이것을 벗어난 것은 키에 넣지 않는다. */
	private static final Pattern SAFE_EXTENSION = Pattern.compile("\\.[A-Za-z0-9]{1,10}$");

	private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");

	private String storageKey;
	private String originalFilename;
	private String contentType;
	private Long size;

	public static ChatAttachment register(Long tenantId, Long roomId, String originalFilename,
		String contentType, Long size) {
		requireStorable(originalFilename, size);

		return ChatAttachment.builder()
			.storageKey(generateStorageKey(tenantId, roomId, originalFilename))
			.originalFilename(originalFilename)
			.contentType(contentType)
			.size(size)
			.build();
	}

	/**
	 * 이미지면 말풍선에 미리보기로, 아니면 파일 카드로 그린다.
	 * <b>사용자가 고르는 값이 아니라 업로드된 {@code contentType}이 정한다.</b>
	 */
	public ChatMessageType messageType() {
		return contentType != null && contentType.startsWith("image/")
			? ChatMessageType.IMAGE
			: ChatMessageType.FILE;
	}

	private static void requireStorable(String originalFilename, Long size) {
		if (originalFilename == null || originalFilename.isBlank() || size == null || size <= 0) {
			throw new CustomException(ErrorCode.CHAT_MESSAGE_EMPTY);
		}
		if (size > MAX_SIZE_BYTES) {
			throw new CustomException(ErrorCode.CHAT_ATTACHMENT_TOO_LARGE);
		}
	}

	/** {@code chat/{tenantId}/{roomId}/{yyyyMM}/{UUID}{확장자}} */
	private static String generateStorageKey(Long tenantId, Long roomId, String originalFilename) {
		return "chat/%d/%d/%s/%s%s".formatted(
			tenantId, roomId, LocalDate.now().format(MONTH), UUID.randomUUID(), extensionOf(originalFilename));
	}

	private static String extensionOf(String originalFilename) {
		Matcher matcher = SAFE_EXTENSION.matcher(originalFilename);
		return matcher.find() ? matcher.group().toLowerCase() : "";
	}
}
