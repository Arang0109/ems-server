package com.ensolution.ems.chat.domain;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 읽음 커서가 <b>앞으로만 간다</b>는 것을 고정한다.
 *
 * <p>위로 스크롤해 옛 메시지를 보는 것은 읽음 취소가 아니고, 여러 탭에서 온 읽음 보고가
 * 뒤바뀐 순서로 도착할 수도 있다. 커서가 되돌아가면 <b>이미 읽은 메시지가 다시 안 읽음으로
 * 돌아와</b> 배지가 살아난다 — 사용자 눈에는 유령 알림이다.
 *
 * <p>비교는 Mongo {@code ObjectId} 문자열의 사전순이다. 앞 4바이트가 초 단위 타임스탬프이고
 * 뒤이어 단조 증가 카운터가 오므로 16진 문자열의 사전순이 곧 생성순이다.
 */
class ChatParticipantReadCursorTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 8, 14, 0);

	private static ChatParticipant participant() {
		return ChatParticipant.join(1L, 100L, 10L);
	}

	@Test
	@DisplayName("처음 읽으면 커서가 생긴다")
	void 첫_읽음은_커서를_만든다() {
		ChatParticipant read = participant().readUpTo("665f0000000000000000000a", NOW);

		assertThat(read.getLastReadMessageId()).isEqualTo("665f0000000000000000000a");
		assertThat(read.getLastReadAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("더 뒤의 메시지를 읽으면 커서가 앞으로 간다")
	void 뒤로_읽으면_전진한다() {
		ChatParticipant read = participant()
			.readUpTo("665f0000000000000000000a", NOW)
			.readUpTo("665f0000000000000000000b", NOW.plusMinutes(1));

		assertThat(read.getLastReadMessageId()).isEqualTo("665f0000000000000000000b");
	}

	@Test
	@DisplayName("이전 메시지를 다시 읽어도 커서는 그대로다")
	void 앞으로_되돌아가지_않는다() {
		ChatParticipant read = participant()
			.readUpTo("665f0000000000000000000b", NOW)
			.readUpTo("665f0000000000000000000a", NOW.plusMinutes(1));

		assertThat(read.getLastReadMessageId()).isEqualTo("665f0000000000000000000b");
		assertThat(read.getLastReadAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("같은 메시지를 다시 읽어도 아무 일도 없다")
	void 같은_위치는_무시한다() {
		ChatParticipant once = participant().readUpTo("665f0000000000000000000a", NOW);
		ChatParticipant twice = once.readUpTo("665f0000000000000000000a", NOW.plusMinutes(1));

		assertThat(twice.getLastReadAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("null 을 읽음으로 보고해도 커서를 지우지 않는다")
	void null은_커서를_지우지_않는다() {
		ChatParticipant read = participant()
			.readUpTo("665f0000000000000000000a", NOW)
			.readUpTo(null, NOW.plusMinutes(1));

		assertThat(read.getLastReadMessageId()).isEqualTo("665f0000000000000000000a");
	}

	@Test
	@DisplayName("나가기는 내 참가 행만 감춘다")
	void 나가면_감춰진다() {
		assertThat(participant().hide().isHidden()).isTrue();
		assertThat(participant().hide().reveal().isHidden()).isFalse();
	}

	@Test
	@DisplayName("메시지 id 형식이 아니면 저장하지 않는다 — 한 번 들어가면 되돌릴 수 없다")
	void 잘못된_형식은_거부한다() {
		// 가장 흔한 실수: clientMessageId(임시 말풍선 키)를 읽음 위치로 보낸다.
		assertThatThrownBy(() -> participant().readUpTo("a3f1-9c2e-4b7d-8f10", NOW))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_INVALID_MESSAGE_ID);
	}

	@Test
	@DisplayName("길이는 맞아도 16진이 아니면 거부한다 — 컬럼에 들어가 버리는 크기라 더 위험하다")
	void 길이만_맞는_값도_거부한다() {
		assertThatThrownBy(() -> participant().readUpTo("zzzzzzzzzzzzzzzzzzzzzzzz", NOW))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_INVALID_MESSAGE_ID);
	}

	@Test
	@DisplayName("거부된 값은 기존 커서를 건드리지 않는다")
	void 거부해도_기존_커서는_그대로다() {
		ChatParticipant read = participant().readUpTo("665f0000000000000000000a", NOW);

		assertThatThrownBy(() -> read.readUpTo("not-an-object-id", NOW.plusMinutes(1)))
			.isInstanceOf(CustomException.class);
		assertThat(read.getLastReadMessageId()).isEqualTo("665f0000000000000000000a");
	}

	@Test
	@DisplayName("대문자 16진도 정상으로 받는다")
	void 대문자_16진도_받는다() {
		assertThat(participant().readUpTo("665F0000000000000000000A", NOW).getLastReadMessageId())
			.isEqualTo("665F0000000000000000000A");
	}
}
