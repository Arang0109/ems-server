package com.ensolution.ems.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 두 사람이 나누는 1:1 대화방.
 * <p>
 * <b>{@code pairKey}가 이 애그리거트의 핵심입니다.</b> "같은 두 사람에게 방은 하나"라는 규칙을
 * 애플리케이션의 존재 검사로는 지킬 수 없습니다 — 두 사람이 동시에 대화를 시작하면 둘 다 "없다"를
 * 보고 둘 다 만듭니다. 그래서 두 id를 정렬해 하나의 문자열로 만들고
 * {@code UNIQUE (tenant_id, pair_key)}로 DB가 막게 합니다. 충돌 흡수는
 * {@code DirectRoomWriter}가 담당합니다.
 * <p>
 * 마지막 메시지 정보({@code lastMessageId}·{@code lastMessagePreview}·{@code lastMessageAt})는
 * 대화방 목록을 그리기 위한 <b>비정규화 사본</b>입니다. 메시지 본문의 진실은 MongoDB
 * {@code chat_messages}에 있고, 여기 있는 것은 목록 한 줄을 그리기 위한 요약입니다.
 * 미읽음 수는 사본으로 두지 않습니다 — MySQL 카운터와 Mongo insert는 2PC를 걸 수 없어
 * 반드시 어긋나므로, 파생값은 저장하지 않고 매번 셉니다.
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ChatRoom {

	/** 목록 한 줄에 보여 줄 미리보기 길이. 컬럼 길이(200)와 맞춘다. */
	private static final int PREVIEW_MAX_LENGTH = 200;

	private Long id;
	private Long tenantId;
	private String pairKey;
	private String lastMessageId;
	private String lastMessagePreview;
	private LocalDateTime lastMessageAt;

	public static ChatRoom openDirect(Long tenantId, Long userA, Long userB) {
		return ChatRoom.builder()
			.tenantId(tenantId)
			.pairKey(pairKeyOf(userA, userB))
			.build();
	}

	/**
	 * 두 사용자 id를 정렬해 만든 방 식별 키. <b>인자 순서가 결과를 바꾸지 않는다</b> —
	 * {@code open(A,B)}와 {@code open(B,A)}가 같은 방으로 수렴해야 하기 때문이다.
	 */
	public static String pairKeyOf(Long userA, Long userB) {
		long min = Math.min(userA, userB);
		long max = Math.max(userA, userB);
		return min + ":" + max;
	}

	/** 목록에 보여 줄 마지막 메시지 요약을 갱신한다. 본문은 Mongo가 갖고 여기엔 미리보기만 남는다. */
	public ChatRoom withLastMessage(String messageId, String preview, LocalDateTime sentAt) {
		return this.toBuilder()
			.lastMessageId(messageId)
			.lastMessagePreview(truncate(preview))
			.lastMessageAt(sentAt)
			.build();
	}

	private static String truncate(String preview) {
		if (preview == null) {
			return null;
		}
		return preview.length() <= PREVIEW_MAX_LENGTH ? preview : preview.substring(0, PREVIEW_MAX_LENGTH);
	}
}
