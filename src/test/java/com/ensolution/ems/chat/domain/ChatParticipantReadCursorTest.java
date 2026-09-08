package com.ensolution.ems.chat.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

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
}
