package com.ensolution.ems.chat.application.service;

import com.ensolution.ems.chat.application.FakeChatParticipantRepository;
import com.ensolution.ems.chat.application.FakeChatRoomRepository;
import com.ensolution.ems.chat.application.FakeUserQuery;
import com.ensolution.ems.chat.application.command.ChatRoomDetail;
import com.ensolution.ems.chat.application.command.ChatRoomListItem;
import com.ensolution.ems.chat.application.command.OpenDirectRoomCommand;
import com.ensolution.ems.chat.application.service.assembler.ChatRoomListAssembler;
import com.ensolution.ems.chat.application.service.support.DirectRoomWriter;
import com.ensolution.ems.chat.application.validator.ChatRoomValidator;
import com.ensolution.ems.chat.domain.ChatRoom;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 1:1 대화방이 지키는 것을 고정한다.
 *
 * <p>첫째, <b>방은 두 사람당 하나</b>. 같은 상대로 두 번 열어도, 인자 순서를 바꿔도 같은 방이어야 한다.
 * 동시 개설로 유니크 제약이 터진 경우까지 포함해 <b>멱등</b>이다 — 사용자가 "대화 시작"을 두 번
 * 누르는 것은 흔한 일이고, 그때마다 방이 늘면 대화가 갈라진다.
 *
 * <p>둘째, <b>tenant 격리가 두 겹</b>이라는 것. 방 자신의 범위와, 개설 시 지정하는 상대의 소속.
 * 후자를 빠뜨리면 다른 고객사 사용자와 대화방이 열리고 이름·부서가 그대로 넘어간다.
 *
 * <p>셋째, <b>참가자가 아닌 접근은 404</b>. 403으로 답하면 "그 방은 있는데 네 것이 아니다"를
 * 알려 주는 셈이라 방의 존재가 드러난다(루트 규칙 13).
 */
class ChatRoomServiceTest {

	private static final long TENANT = 1L;
	private static final long OTHER_TENANT = 2L;

	private static final long ME = 10L;
	private static final long PEER = 20L;
	private static final long STRANGER = 30L;
	private static final long OTHER_TENANT_USER = 99L;

	private final FakeChatRoomRepository roomRepository = new FakeChatRoomRepository();
	private final FakeChatParticipantRepository participantRepository = new FakeChatParticipantRepository();
	private final FakeUserQuery userQuery = new FakeUserQuery();

	private final ChatRoomService chatRoomService = new ChatRoomService(
		roomRepository,
		participantRepository,
		new ChatRoomValidator(userQuery),
		new DirectRoomWriter(roomRepository, participantRepository),
		new ChatRoomListAssembler(userQuery)
	);

	ChatRoomServiceTest() {
		userQuery.given(ME, TENANT, "나", "측정1팀");
		userQuery.given(PEER, TENANT, "상대", "측정2팀");
		userQuery.given(STRANGER, TENANT, "제삼자", "분석팀");
		userQuery.given(OTHER_TENANT_USER, OTHER_TENANT, "남의회사", "영업팀");
	}

	private static OpenDirectRoomCommand openCommand(Long tenantId, Long requesterId, Long counterpartId) {
		return new OpenDirectRoomCommand(tenantId, requesterId, counterpartId);
	}

	@Nested
	@DisplayName("방 열기 — 두 사람당 하나")
	class OpenDirectRoom {

		@Test
		@DisplayName("같은 상대로 두 번 열어도 같은 방이다")
		void 두_번_열어도_같은_방이다() {
			ChatRoomDetail first = chatRoomService.openDirectRoom(openCommand(TENANT, ME, PEER));
			ChatRoomDetail second = chatRoomService.openDirectRoom(openCommand(TENANT, ME, PEER));

			assertThat(second.roomId()).isEqualTo(first.roomId());
			assertThat(roomRepository.count()).isEqualTo(1);
			assertThat(participantRepository.count()).isEqualTo(2);
		}

		@Test
		@DisplayName("상대가 먼저 열어 둔 방에 내가 들어가도 같은 방이다 — 인자 순서가 결과를 바꾸지 않는다")
		void 순서를_바꿔도_같은_방이다() {
			ChatRoomDetail opened = chatRoomService.openDirectRoom(openCommand(TENANT, PEER, ME));
			ChatRoomDetail reopened = chatRoomService.openDirectRoom(openCommand(TENANT, ME, PEER));

			assertThat(reopened.roomId()).isEqualTo(opened.roomId());
			assertThat(roomRepository.count()).isEqualTo(1);
		}

		@Test
		@DisplayName("동시 개설로 유니크 제약이 터져도 상대가 만든 방을 돌려준다")
		void 동시_개설_충돌을_흡수한다() {
			// 경쟁에서 진 상황: 내가 insert 하려는 순간 이미 같은 pairKey 가 들어와 있다.
			ChatRoom winner = roomRepository.given(TENANT, ME, PEER);
			participantRepository.given(TENANT, winner.getId(), ME);
			participantRepository.given(TENANT, winner.getId(), PEER);
			roomRepository.failNextSaveWithConflict();

			ChatRoomDetail detail = chatRoomService.openDirectRoom(openCommand(TENANT, ME, PEER));

			assertThat(detail.roomId()).isEqualTo(winner.getId());
			assertThat(roomRepository.count()).isEqualTo(1);
		}

		@Test
		@DisplayName("자기 자신과는 방을 만들 수 없다")
		void 자기_자신과는_만들_수_없다() {
			assertThatThrownBy(() -> chatRoomService.openDirectRoom(openCommand(TENANT, ME, ME)))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_SELF_ROOM_NOT_ALLOWED);

			assertThat(roomRepository.count()).isZero();
		}

		@Test
		@DisplayName("다른 테넌트 사용자와는 방을 만들 수 없다")
		void 남의_테넌트_사용자와는_만들_수_없다() {
			assertThatThrownBy(() ->
				chatRoomService.openDirectRoom(openCommand(TENANT, ME, OTHER_TENANT_USER)))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

			assertThat(roomRepository.count()).isZero();
		}

		@Test
		@DisplayName("존재하지 않는 사용자와도 만들 수 없다 — 타 테넌트와 같은 응답으로 존재를 숨긴다")
		void 없는_사용자와는_만들_수_없다() {
			assertThatThrownBy(() -> chatRoomService.openDirectRoom(openCommand(TENANT, ME, 404L)))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
		}

		@Test
		@DisplayName("나갔던 방을 다시 열면 목록에 되돌아온다")
		void 나간_방을_다시_열면_되살아난다() {
			Long roomId = chatRoomService.openDirectRoom(openCommand(TENANT, ME, PEER)).roomId();
			chatRoomService.hideRoom(roomId, ME, TENANT);
			assertThat(chatRoomService.getRoomList(ME, TENANT)).isEmpty();

			chatRoomService.openDirectRoom(openCommand(TENANT, ME, PEER));

			assertThat(chatRoomService.getRoomList(ME, TENANT))
				.extracting(ChatRoomListItem::roomId)
				.containsExactly(roomId);
		}
	}

	@Nested
	@DisplayName("조회 — 참가자와 tenant 범위")
	class Reading {

		@Test
		@DisplayName("상대의 이름과 부서가 채워진다 — 방 자신은 그것을 모른다")
		void 상대_정보가_조립된다() {
			Long roomId = chatRoomService.openDirectRoom(openCommand(TENANT, ME, PEER)).roomId();

			ChatRoomDetail detail = chatRoomService.getRoom(roomId, ME, TENANT);

			assertThat(detail.peer().userId()).isEqualTo(PEER);
			assertThat(detail.peer().name()).isEqualTo("상대");
			assertThat(detail.peer().department()).isEqualTo("측정2팀");
		}

		@Test
		@DisplayName("참가자가 아니면 방이 없다고 답한다 — 403이 아니다")
		void 참가자가_아니면_404다() {
			Long roomId = chatRoomService.openDirectRoom(openCommand(TENANT, ME, PEER)).roomId();

			assertThatThrownBy(() -> chatRoomService.getRoom(roomId, STRANGER, TENANT))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_ROOM_NOT_FOUND);
		}

		@Test
		@DisplayName("다른 테넌트에서는 같은 roomId 로도 조회되지 않는다")
		void 남의_테넌트에서는_조회되지_않는다() {
			Long roomId = chatRoomService.openDirectRoom(openCommand(TENANT, ME, PEER)).roomId();

			assertThatThrownBy(() -> chatRoomService.getRoom(roomId, ME, OTHER_TENANT))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_ROOM_NOT_FOUND);
		}

		@Test
		@DisplayName("목록에는 내가 참가한 방만 들어간다")
		void 목록은_내_방만_담는다() {
			chatRoomService.openDirectRoom(openCommand(TENANT, ME, PEER));
			chatRoomService.openDirectRoom(openCommand(TENANT, PEER, STRANGER));

			assertThat(chatRoomService.getRoomList(ME, TENANT))
				.extracting(item -> item.peer().userId())
				.containsExactly(PEER);
		}

		@Test
		@DisplayName("나간 방은 내 목록에서만 빠진다 — 상대는 그대로 본다")
		void 나가면_나에게만_사라진다() {
			Long roomId = chatRoomService.openDirectRoom(openCommand(TENANT, ME, PEER)).roomId();

			chatRoomService.hideRoom(roomId, ME, TENANT);

			assertThat(chatRoomService.getRoomList(ME, TENANT)).isEmpty();
			assertThat(chatRoomService.getRoomList(PEER, TENANT))
				.extracting(ChatRoomListItem::roomId)
				.containsExactly(roomId);
		}

		@Test
		@DisplayName("참가자가 아니면 나가기도 할 수 없다")
		void 참가자가_아니면_나갈_수_없다() {
			Long roomId = chatRoomService.openDirectRoom(openCommand(TENANT, ME, PEER)).roomId();

			assertThatThrownBy(() -> chatRoomService.hideRoom(roomId, STRANGER, TENANT))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_ROOM_NOT_FOUND);
		}
	}
}
