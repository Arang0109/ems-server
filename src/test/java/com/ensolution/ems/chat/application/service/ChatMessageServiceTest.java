package com.ensolution.ems.chat.application.service;

import com.ensolution.ems.chat.application.FakeChatMessageRepository;
import com.ensolution.ems.chat.application.FakeChatParticipantRepository;
import com.ensolution.ems.chat.application.FakeChatRoomRepository;
import com.ensolution.ems.chat.application.FakeUserQuery;
import com.ensolution.ems.chat.application.RecordingChatEventBroadcaster;
import com.ensolution.ems.chat.application.RecordingFileStorageClient;
import com.ensolution.ems.chat.application.command.AttachmentUpload;
import com.ensolution.ems.chat.application.command.ChatAttachmentFile;
import com.ensolution.ems.chat.application.command.ChatAttachmentInfo;
import com.ensolution.ems.chat.application.command.ChatMessageListItem;
import com.ensolution.ems.chat.application.command.ChatMessagePage;
import com.ensolution.ems.chat.application.command.SendMessageCommand;
import com.ensolution.ems.chat.application.event.ChatMessagePayload;
import com.ensolution.ems.chat.application.service.support.ChatAttachmentWriter;
import com.ensolution.ems.chat.application.service.support.ChatEventPublisher;
import com.ensolution.ems.chat.domain.ChatMessageType;
import com.ensolution.ems.chat.domain.ChatRoom;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 메시지 전송과 이력 조회가 지키는 것을 고정한다.
 *
 * <p>첫째, <b>참가자가 아니면 보낼 수도 읽을 수도 없다</b>. 방 id 는 순번이라 추측하기 쉽고,
 * 이 검증이 빠지면 남의 대화에 끼어들거나 통째로 읽어 갈 수 있다. 응답은 403이 아니라
 * {@code CHAT_ROOM_NOT_FOUND}여야 방의 존재조차 드러나지 않는다.
 *
 * <p>둘째, <b>커서 페이징의 경계</b>. 커서는 "이 id 보다 앞"이므로 커서로 준 메시지 자신은
 * 다음 페이지에 다시 나오면 안 된다. 경계를 한 칸 잘못 잡으면 스크롤할 때마다 같은 메시지가
 * 중복되거나 한 건씩 사라진다.
 *
 * <p>셋째, <b>전송이 방의 요약을 갱신하고 감춰 둔 방을 되살린다</b>는 것. 메시지가 왔는데 상대의
 * 목록에 방이 없으면 볼 방법이 없다.
 */
class ChatMessageServiceTest {

	private static final long TENANT = 1L;
	private static final long OTHER_TENANT = 2L;

	private static final long ME = 10L;
	private static final long PEER = 20L;
	private static final long STRANGER = 30L;

	private final FakeChatRoomRepository roomRepository = new FakeChatRoomRepository();
	private final FakeChatParticipantRepository participantRepository = new FakeChatParticipantRepository();
	private final FakeChatMessageRepository messageRepository = new FakeChatMessageRepository();
	private final FakeUserQuery userQuery = new FakeUserQuery();

	private final RecordingChatEventBroadcaster broadcaster = new RecordingChatEventBroadcaster();

	private final RecordingFileStorageClient fileStorage = new RecordingFileStorageClient();

	private final ChatMessageService chatMessageService = new ChatMessageService(
		roomRepository, participantRepository, messageRepository, userQuery,
		new ChatEventPublisher(broadcaster), new ChatAttachmentWriter(fileStorage));

	private final Long roomId;

	ChatMessageServiceTest() {
		userQuery.given(ME, TENANT, "나", "측정1팀");
		userQuery.given(PEER, TENANT, "상대", "측정2팀");
		userQuery.given(STRANGER, TENANT, "제삼자", "분석팀");

		ChatRoom room = roomRepository.given(TENANT, ME, PEER);
		participantRepository.given(TENANT, room.getId(), ME);
		participantRepository.given(TENANT, room.getId(), PEER);
		this.roomId = room.getId();
	}

	private SendMessageCommand sendCommand(Long tenantId, Long senderId, String content) {
		return new SendMessageCommand(tenantId, roomId, senderId, content, null, null);
	}

	@Nested
	@DisplayName("전송")
	class Sending {

		@Test
		@DisplayName("보낸 사람의 이름이 함께 실린다 — 메시지 문서는 id 만 갖는다")
		void 보낸사람_이름이_조립된다() {
			ChatMessageListItem sent = chatMessageService.sendMessage(sendCommand(TENANT, ME, "안녕하세요"));

			assertThat(sent.senderId()).isEqualTo(ME);
			assertThat(sent.senderName()).isEqualTo("나");
			assertThat(sent.content()).isEqualTo("안녕하세요");
			assertThat(sent.messageId()).isNotNull();
		}

		@Test
		@DisplayName("clientMessageId 는 해석하지 않고 그대로 돌려준다")
		void 클라이언트_키는_그대로_돌아온다() {
			ChatMessageListItem sent = chatMessageService.sendMessage(
				new SendMessageCommand(TENANT, roomId, ME, "안녕", "tmp-uuid-1", null));

			assertThat(sent.clientMessageId()).isEqualTo("tmp-uuid-1");
		}

		@Test
		@DisplayName("방의 마지막 메시지 요약이 갱신된다")
		void 방_요약이_갱신된다() {
			ChatMessageListItem sent = chatMessageService.sendMessage(sendCommand(TENANT, ME, "회의 30분 뒤"));

			ChatRoom room = roomRepository.findById(roomId, TENANT);
			assertThat(room.getLastMessageId()).isEqualTo(sent.messageId());
			assertThat(room.getLastMessagePreview()).isEqualTo("회의 30분 뒤");
			assertThat(room.getLastMessageAt()).isNotNull();
		}

		@Test
		@DisplayName("상대가 나가 둔 방도 새 메시지가 오면 다시 나타난다")
		void 감춘_방이_되살아난다() {
			participantRepository.save(
				participantRepository.findByRoomIdAndUserId(roomId, PEER, TENANT).hide());
			assertThat(participantRepository.findAllVisibleByUserId(PEER, TENANT)).isEmpty();

			chatMessageService.sendMessage(sendCommand(TENANT, ME, "계세요?"));

			assertThat(participantRepository.findAllVisibleByUserId(PEER, TENANT)).hasSize(1);
		}

		@Test
		@DisplayName("참가자가 아니면 보낼 수 없다 — 방이 없다고 답한다")
		void 참가자가_아니면_보낼_수_없다() {
			assertThatThrownBy(() -> chatMessageService.sendMessage(sendCommand(TENANT, STRANGER, "끼어들기")))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_ROOM_NOT_FOUND);
		}

		@Test
		@DisplayName("다른 테넌트에서는 같은 roomId 로도 보낼 수 없다")
		void 남의_테넌트에서는_보낼_수_없다() {
			assertThatThrownBy(() -> chatMessageService.sendMessage(sendCommand(OTHER_TENANT, ME, "안녕")))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_ROOM_NOT_FOUND);
		}

		@Test
		@DisplayName("빈 내용은 보낼 수 없다")
		void 빈_메시지는_보낼_수_없다() {
			assertThatThrownBy(() -> chatMessageService.sendMessage(sendCommand(TENANT, ME, "   ")))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_MESSAGE_EMPTY);
		}
	}

	@Nested
	@DisplayName("이력 조회 — 커서 페이징")
	class Paging {

		private void givenMessages(int count) {
			for (int i = 1; i <= count; i++) {
				chatMessageService.sendMessage(sendCommand(TENANT, ME, "메시지 " + i));
			}
		}

		@Test
		@DisplayName("최신순으로 준다")
		void 최신순이다() {
			givenMessages(3);

			ChatMessagePage page = chatMessageService.getMessages(roomId, ME, TENANT, null, 10);

			assertThat(page.messages())
				.extracting(ChatMessageListItem::content)
				.containsExactly("메시지 3", "메시지 2", "메시지 1");
			assertThat(page.hasMore()).isFalse();
			assertThat(page.nextCursor()).isNull();
		}

		@Test
		@DisplayName("요청 크기를 넘으면 hasMore 와 커서를 준다")
		void 다음_페이지가_있으면_알린다() {
			givenMessages(5);

			ChatMessagePage page = chatMessageService.getMessages(roomId, ME, TENANT, null, 2);

			assertThat(page.messages()).hasSize(2);
			assertThat(page.hasMore()).isTrue();
			assertThat(page.nextCursor()).isEqualTo(page.messages().get(1).messageId());
		}

		@Test
		@DisplayName("커서로 준 메시지는 다음 페이지에 다시 나오지 않는다")
		void 커서_경계에서_중복되지_않는다() {
			givenMessages(5);

			ChatMessagePage first = chatMessageService.getMessages(roomId, ME, TENANT, null, 2);
			ChatMessagePage second =
				chatMessageService.getMessages(roomId, ME, TENANT, first.nextCursor(), 2);

			assertThat(first.messages())
				.extracting(ChatMessageListItem::content)
				.containsExactly("메시지 5", "메시지 4");
			assertThat(second.messages())
				.extracting(ChatMessageListItem::content)
				.containsExactly("메시지 3", "메시지 2");
		}

		@Test
		@DisplayName("끝까지 넘기면 더 없다고 알린다")
		void 마지막_페이지를_알린다() {
			givenMessages(3);

			ChatMessagePage first = chatMessageService.getMessages(roomId, ME, TENANT, null, 2);
			ChatMessagePage last =
				chatMessageService.getMessages(roomId, ME, TENANT, first.nextCursor(), 2);

			assertThat(last.messages()).extracting(ChatMessageListItem::content).containsExactly("메시지 1");
			assertThat(last.hasMore()).isFalse();
			assertThat(last.nextCursor()).isNull();
		}

		@Test
		@DisplayName("size 를 주지 않으면 기본값으로 읽는다")
		void 기본_크기로_읽는다() {
			givenMessages(3);

			assertThat(chatMessageService.getMessages(roomId, ME, TENANT, null, null).messages())
				.hasSize(3);
		}

		@Test
		@DisplayName("참가자가 아니면 이력을 읽을 수 없다")
		void 참가자가_아니면_읽을_수_없다() {
			givenMessages(1);

			assertThatThrownBy(() -> chatMessageService.getMessages(roomId, STRANGER, TENANT, null, 10))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_ROOM_NOT_FOUND);
		}

		@Test
		@DisplayName("다른 방의 메시지가 섞이지 않는다")
		void 다른_방_메시지는_섞이지_않는다() {
			givenMessages(2);
			ChatRoom otherRoom = roomRepository.given(TENANT, ME, STRANGER);
			participantRepository.given(TENANT, otherRoom.getId(), ME);
			participantRepository.given(TENANT, otherRoom.getId(), STRANGER);
			chatMessageService.sendMessage(
				new SendMessageCommand(TENANT, otherRoom.getId(), ME, "다른 방 메시지", null, null));

			ChatMessagePage page = chatMessageService.getMessages(roomId, ME, TENANT, null, 10);

			assertThat(page.messages())
				.extracting(ChatMessageListItem::content)
				.containsExactly("메시지 2", "메시지 1");
		}

		@Test
		@DisplayName("대화가 없으면 빈 페이지다")
		void 대화가_없으면_비어있다() {
			ChatMessagePage page = chatMessageService.getMessages(roomId, ME, TENANT, null, 10);

			assertThat(page.messages()).isEmpty();
			assertThat(page.hasMore()).isFalse();
		}

		@Test
		@DisplayName("과도한 size 요청은 상한으로 자른다")
		void 크기_상한을_넘지_않는다() {
			givenMessages(3);

			List<ChatMessageListItem> messages =
				chatMessageService.getMessages(roomId, ME, TENANT, null, 100_000).messages();

			assertThat(messages).hasSize(3);
		}
	}

	@Nested
	@DisplayName("실시간 알림")
	class Broadcasting {

		@Test
		@DisplayName("방 참가자 전원에게 간다 — 발신자 자신도 포함한다")
		void 참가자_전원에게_간다() {
			chatMessageService.sendMessage(sendCommand(TENANT, ME, "안녕"));

			assertThat(broadcaster.messages()).singleElement()
				.extracting(RecordingChatEventBroadcaster.Sent::recipients)
				.asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.list(String.class))
				// 발신자에게도 보낸다. 다른 탭·기기의 화면을 맞춰야 한다.
				.containsExactlyInAnyOrder("user" + ME, "user" + PEER);
		}

		@Test
		@DisplayName("본문을 그대로 싣는다 — 수신 측이 재조회하지 않아도 그릴 수 있어야 한다")
		void 본문이_실린다() {
			chatMessageService.sendMessage(
				new SendMessageCommand(TENANT, roomId, ME, "3번 굴뚝 끝", "tmp-1", null));

			ChatMessagePayload payload = broadcaster.messages().get(0).payload();
			assertThat(payload.roomId()).isEqualTo(roomId);
			assertThat(payload.senderId()).isEqualTo(ME);
			assertThat(payload.senderName()).isEqualTo("나");
			assertThat(payload.content()).isEqualTo("3번 굴뚝 끝");
			assertThat(payload.clientMessageId()).isEqualTo("tmp-1");
			assertThat(payload.messageId()).isNotNull();
		}

		@Test
		@DisplayName("전송이 거부되면 알림도 나가지 않는다")
		void 실패하면_알리지_않는다() {
			assertThatThrownBy(() -> chatMessageService.sendMessage(sendCommand(TENANT, STRANGER, "끼어들기")))
				.isInstanceOf(CustomException.class);

			assertThat(broadcaster.messages()).isEmpty();
		}

		@Test
		@DisplayName("조회는 알림을 내지 않는다")
		void 조회는_알리지_않는다() {
			chatMessageService.sendMessage(sendCommand(TENANT, ME, "안녕"));
			broadcaster.messages().clear();

			chatMessageService.getMessages(roomId, ME, TENANT, null, 10);

			assertThat(broadcaster.messages()).isEmpty();
		}
	}

	@Nested
	@DisplayName("첨부")
	class Attachments {

		private static final byte[] BYTES = "hello".getBytes(java.nio.charset.StandardCharsets.UTF_8);

		private SendMessageCommand attachmentCommand(String filename, String contentType, long size) {
			return new SendMessageCommand(TENANT, roomId, ME, null, null,
				new AttachmentUpload(filename, contentType, size, BYTES));
		}

		@Test
		@DisplayName("이미지면 IMAGE, 아니면 FILE 이다 — 사용자가 고르는 값이 아니다")
		void 종류는_contentType이_정한다() {
			assertThat(chatMessageService.sendMessage(
				attachmentCommand("사진.png", "image/png", 5L)).type())
				.isEqualTo(ChatMessageType.IMAGE);

			assertThat(chatMessageService.sendMessage(
				attachmentCommand("보고서.pdf", "application/pdf", 5L)).type())
				.isEqualTo(ChatMessageType.FILE);
		}

		@Test
		@DisplayName("본문 없이 파일만 보낼 수 있다 — 본문은 캡션이다")
		void 파일만_보낼_수_있다() {
			ChatMessageListItem sent = chatMessageService.sendMessage(
				attachmentCommand("보고서.pdf", "application/pdf", 5L));

			assertThat(sent.content()).isNull();
			assertThat(sent.attachment().filename()).isEqualTo("보고서.pdf");
			assertThat(sent.attachment().size()).isEqualTo(5L);
		}

		@Test
		@DisplayName("실물이 보관소에 쓰인다")
		void 실물이_보관된다() {
			chatMessageService.sendMessage(attachmentCommand("보고서.pdf", "application/pdf", 5L));

			assertThat(fileStorage.stored()).hasSize(1);
			assertThat(fileStorage.stored().keySet()).allSatisfy(key ->
				// 문서 보관소와 섞이지 않도록 chat/ 아래에 두고, 원본 파일명은 경로에 넣지 않는다.
				assertThat(key).startsWith("chat/" + TENANT + "/" + roomId + "/")
					.doesNotContain("보고서")
					.endsWith(".pdf"));
		}

		@Test
		@DisplayName("보관소 키는 응답에 나가지 않는다 — 그 자체가 접근 경로가 된다")
		void 보관소_키는_노출되지_않는다() {
			ChatMessageListItem sent = chatMessageService.sendMessage(
				attachmentCommand("보고서.pdf", "application/pdf", 5L));

			assertThat(sent.attachment().filename()).isEqualTo("보고서.pdf");
			assertThat(sent.attachment().contentType()).isEqualTo("application/pdf");
			// ChatAttachmentInfo 에는 storageKey 필드가 없다. 목록 응답도 같은 타입을 쓴다.
			assertThat(ChatAttachmentInfo.class.getRecordComponents())
				.extracting(java.lang.reflect.RecordComponent::getName)
				.containsExactly("filename", "contentType", "size");
		}

		@Test
		@DisplayName("10MB 를 넘으면 거부하고 보관소에도 쓰지 않는다")
		void 크기_상한을_넘으면_거부한다() {
			assertThatThrownBy(() -> chatMessageService.sendMessage(
				attachmentCommand("큰파일.zip", "application/zip", 10L * 1024 * 1024 + 1)))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_ATTACHMENT_TOO_LARGE);

			assertThat(fileStorage.stored()).isEmpty();
		}

		@Test
		@DisplayName("참가자가 아니면 첨부를 보낼 수 없다 — 보관소에도 쓰지 않는다")
		void 참가자가_아니면_보낼_수_없다() {
			SendMessageCommand command = new SendMessageCommand(TENANT, roomId, STRANGER, null, null,
				new AttachmentUpload("보고서.pdf", "application/pdf", 5L, BYTES));

			assertThatThrownBy(() -> chatMessageService.sendMessage(command))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_ROOM_NOT_FOUND);

			assertThat(fileStorage.stored()).isEmpty();
		}

		@Test
		@DisplayName("참가자는 실물을 받을 수 있다")
		void 참가자는_받을_수_있다() {
			String messageId = chatMessageService.sendMessage(
				attachmentCommand("보고서.pdf", "application/pdf", 5L)).messageId();

			ChatAttachmentFile file =
				chatMessageService.getAttachment(roomId, messageId, PEER, TENANT);

			assertThat(file.filename()).isEqualTo("보고서.pdf");
			assertThat(file.contentType()).isEqualTo("application/pdf");
			assertThat(file.content()).isEqualTo(BYTES);
		}

		@Test
		@DisplayName("참가자가 아니면 실물도 받을 수 없다")
		void 참가자가_아니면_받을_수_없다() {
			String messageId = chatMessageService.sendMessage(
				attachmentCommand("보고서.pdf", "application/pdf", 5L)).messageId();

			assertThatThrownBy(() ->
				chatMessageService.getAttachment(roomId, messageId, STRANGER, TENANT))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_ROOM_NOT_FOUND);
		}

		@Test
		@DisplayName("첨부 없는 메시지의 다운로드는 잘못된 요청이지 서버 오류가 아니다")
		void 첨부가_없으면_404다() {
			String messageId = chatMessageService.sendMessage(sendCommand(TENANT, ME, "그냥 텍스트")).messageId();

			assertThatThrownBy(() -> chatMessageService.getAttachment(roomId, messageId, ME, TENANT))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_ATTACHMENT_NOT_FOUND);
		}

		@Test
		@DisplayName("메타는 있는데 실물이 없으면 보관소 오류다 — 첨부 없음과 구분한다")
		void 실물이_없으면_보관소_오류다() {
			String messageId = chatMessageService.sendMessage(
				attachmentCommand("보고서.pdf", "application/pdf", 5L)).messageId();
			java.util.List.copyOf(fileStorage.stored().keySet()).forEach(fileStorage::evict);

			assertThatThrownBy(() -> chatMessageService.getAttachment(roomId, messageId, ME, TENANT))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.STORAGE_FILE_NOT_FOUND);
		}

		@Test
		@DisplayName("다른 방의 messageId 로는 받을 수 없다 — 참가자 확인만으로는 부족하다")
		void 다른_방의_첨부는_받을_수_없다() {
			String messageId = chatMessageService.sendMessage(
				attachmentCommand("보고서.pdf", "application/pdf", 5L)).messageId();

			ChatRoom otherRoom = roomRepository.given(TENANT, ME, STRANGER);
			participantRepository.given(TENANT, otherRoom.getId(), ME);
			participantRepository.given(TENANT, otherRoom.getId(), STRANGER);

			assertThatThrownBy(() ->
				chatMessageService.getAttachment(otherRoom.getId(), messageId, ME, TENANT))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_MESSAGE_NOT_FOUND);
		}

		@Test
		@DisplayName("목록 요약에는 사진·파일로 나온다")
		void 요약은_종류로_표시된다() {
			chatMessageService.sendMessage(attachmentCommand("사진.png", "image/png", 5L));

			assertThat(roomRepository.findById(roomId, TENANT).getLastMessagePreview()).isEqualTo("사진");
		}
	}
}
