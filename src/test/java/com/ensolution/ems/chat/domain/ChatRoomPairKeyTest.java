package com.ensolution.ems.chat.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 대화방의 두 불변식을 고정한다.
 *
 * <p>하나는 <b>{@code pairKey}가 인자 순서에 무관</b>하다는 것. "같은 두 사람에게 방은 하나"를
 * 지키는 것은 {@code UNIQUE (tenant_id, pair_key)}인데, 그 제약이 걸리려면 A가 만든 키와 B가 만든
 * 키가 <b>같은 문자열</b>이어야 한다. 정렬을 빠뜨리면 제약은 그대로 있는데 아무것도 막지 못한다.
 *
 * <p>다른 하나는 <b>미리보기가 컬럼 길이를 넘지 않는다</b>는 것. 긴 메시지를 그대로 넣으면
 * 저장 시점에 터지는데, 그 실패는 메시지 전송 트랜잭션 전체를 되돌린다.
 */
class ChatRoomPairKeyTest {

	@Nested
	@DisplayName("pairKey — 두 사람을 하나의 키로")
	class PairKey {

		@Test
		@DisplayName("인자 순서를 바꿔도 같은 키다")
		void 순서가_결과를_바꾸지_않는다() {
			assertThat(ChatRoom.pairKeyOf(37L, 12L)).isEqualTo(ChatRoom.pairKeyOf(12L, 37L));
		}

		@Test
		@DisplayName("작은 id 가 앞에 온다")
		void 정렬된_형태다() {
			assertThat(ChatRoom.pairKeyOf(37L, 12L)).isEqualTo("12:37");
		}

		@Test
		@DisplayName("다른 조합은 다른 키다")
		void 다른_조합은_구분된다() {
			assertThat(ChatRoom.pairKeyOf(1L, 23L)).isNotEqualTo(ChatRoom.pairKeyOf(12L, 3L));
		}

		@Test
		@DisplayName("방을 열면 그 키가 붙는다")
		void 개설_시_키가_붙는다() {
			ChatRoom room = ChatRoom.openDirect(1L, 37L, 12L);

			assertThat(room.getPairKey()).isEqualTo("12:37");
			assertThat(room.getTenantId()).isEqualTo(1L);
		}
	}

	@Nested
	@DisplayName("마지막 메시지 요약")
	class LastMessage {

		@Test
		@DisplayName("긴 본문은 컬럼 길이(200)에 맞춰 자른다")
		void 미리보기는_잘린다() {
			ChatRoom room = ChatRoom.openDirect(1L, 1L, 2L)
				.withLastMessage("abc", "가".repeat(500), LocalDateTime.now());

			assertThat(room.getLastMessagePreview()).hasSize(200);
		}

		@Test
		@DisplayName("짧은 본문은 그대로 둔다")
		void 짧으면_그대로다() {
			ChatRoom room = ChatRoom.openDirect(1L, 1L, 2L)
				.withLastMessage("abc", "안녕하세요", LocalDateTime.now());

			assertThat(room.getLastMessagePreview()).isEqualTo("안녕하세요");
			assertThat(room.getLastMessageId()).isEqualTo("abc");
		}
	}
}
