package com.ensolution.ems.chat.application.service;

import com.ensolution.ems.chat.application.FakePresenceStore;
import com.ensolution.ems.chat.application.FakeUserQuery;
import com.ensolution.ems.chat.application.RecordingChatEventBroadcaster;
import com.ensolution.ems.chat.application.command.ActiveSession;
import com.ensolution.ems.chat.application.command.ChatContactListItem;
import com.ensolution.ems.chat.application.service.support.ChatEventPublisher;
import com.ensolution.ems.chat.domain.PresenceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 접속 상태가 지키는 것을 고정한다.
 *
 * <p>첫째, <b>다중 탭</b>. 한 사람이 창을 여러 개 여는 것은 흔한 일이고, 그중 하나를 닫았다고
 * 오프라인이 되면 상대 화면의 점이 깜빡인다. 상태 변화 알림도 <b>실제로 바뀐 순간에만</b>
 * 나가야 한다 — 탭을 열 때마다 테넌트 전원에게 프레임이 날아가서는 안 된다.
 *
 * <p>둘째, <b>tenant 격리</b>. 다른 고객사 사람의 접속 여부가 보이면 그 자체가 정보 노출이다.
 *
 * <p>셋째, <b>연락처 목록이 노출하는 필드</b>. {@code /api/admin/members} 를 재사용하지 않은 이유가
 * email·tel 을 내보내지 않기 위해서인데, 그 결정이 코드에서 지켜지는지 고정한다.
 */
class ChatPresenceServiceTest {

	private static final long TENANT = 1L;
	private static final long OTHER_TENANT = 2L;

	private static final long ME = 10L;
	private static final long PEER = 20L;
	private static final long OTHER_TENANT_USER = 99L;

	private final FakePresenceStore presenceStore = new FakePresenceStore();
	private final FakeUserQuery userQuery = new FakeUserQuery();
	private final RecordingChatEventBroadcaster broadcaster = new RecordingChatEventBroadcaster();

	private final ChatPresenceService chatPresenceService = new ChatPresenceService(
		presenceStore, userQuery, new ChatEventPublisher(broadcaster));

	ChatPresenceServiceTest() {
		userQuery.given(ME, TENANT, "나", "측정1팀");
		userQuery.given(PEER, TENANT, "상대", "측정2팀");
		userQuery.given(OTHER_TENANT_USER, OTHER_TENANT, "남의회사", "영업팀");
	}

	@Nested
	@DisplayName("세션 — 다중 탭")
	class Sessions {

		@Test
		@DisplayName("첫 세션에서만 온라인을 알린다")
		void 첫_세션에서만_알린다() {
			chatPresenceService.sessionConnected(TENANT, ME, "s1");
			chatPresenceService.sessionConnected(TENANT, ME, "s2");

			assertThat(broadcaster.presences()).hasSize(1);
			assertThat(broadcaster.presences().get(0).payload().status()).isEqualTo(PresenceStatus.ONLINE);
		}

		@Test
		@DisplayName("탭 두 개 중 하나만 닫으면 여전히 온라인이다")
		void 탭_하나를_닫아도_온라인이다() {
			chatPresenceService.sessionConnected(TENANT, ME, "s1");
			chatPresenceService.sessionConnected(TENANT, ME, "s2");
			broadcaster.presences().clear();

			chatPresenceService.sessionDisconnected(TENANT, ME, "s1");

			assertThat(broadcaster.presences()).isEmpty();
			assertThat(presenceStore.onlineUserIds(TENANT)).containsExactly(ME);
		}

		@Test
		@DisplayName("마지막 세션이 끊길 때만 오프라인을 알린다")
		void 마지막_세션에서만_오프라인이다() {
			chatPresenceService.sessionConnected(TENANT, ME, "s1");
			chatPresenceService.sessionConnected(TENANT, ME, "s2");
			broadcaster.presences().clear();

			chatPresenceService.sessionDisconnected(TENANT, ME, "s1");
			chatPresenceService.sessionDisconnected(TENANT, ME, "s2");

			assertThat(broadcaster.presences()).hasSize(1);
			assertThat(broadcaster.presences().get(0).payload().status()).isEqualTo(PresenceStatus.OFFLINE);
			assertThat(presenceStore.onlineUserIds(TENANT)).isEmpty();
		}

		@Test
		@DisplayName("알림은 자기 자신을 뺀 접속 중인 사람에게만 간다")
		void 접속자에게만_알린다() {
			chatPresenceService.sessionConnected(TENANT, PEER, "peer-1");
			broadcaster.presences().clear();

			chatPresenceService.sessionConnected(TENANT, ME, "s1");

			assertThat(broadcaster.presences()).singleElement()
				.satisfies(sent -> {
					// 접속해 있지 않은 사람에게 보내 봐야 브로커가 버린다.
					assertThat(sent.recipients()).containsExactly("user" + PEER);
					assertThat(sent.payload().userId()).isEqualTo(ME);
				});
		}

		@Test
		@DisplayName("다른 테넌트에는 알리지 않는다")
		void 남의_테넌트에는_알리지_않는다() {
			chatPresenceService.sessionConnected(OTHER_TENANT, OTHER_TENANT_USER, "other-1");
			broadcaster.presences().clear();

			chatPresenceService.sessionConnected(TENANT, ME, "s1");

			assertThat(broadcaster.presences()).singleElement()
				.extracting(sent -> sent.recipients())
				.asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.list(String.class))
				.isEmpty();
		}

		@Test
		@DisplayName("주기 갱신이 살아 있는 세션의 만료를 미룬다")
		void 주기_갱신이_만료를_미룬다() {
			chatPresenceService.refreshActiveSessions(List.of(
				new ActiveSession(TENANT, ME, "s1"),
				new ActiveSession(TENANT, PEER, "peer-1")));

			assertThat(presenceStore.touchedUserIds()).containsExactlyInAnyOrder(ME, PEER);
		}
	}

	@Nested
	@DisplayName("연락처 목록")
	class Contacts {

		@Test
		@DisplayName("자기 자신은 빠진다")
		void 자기_자신은_빠진다() {
			assertThat(chatPresenceService.getContacts(ME, TENANT))
				.extracting(ChatContactListItem::userId)
				.containsExactly(PEER);
		}

		@Test
		@DisplayName("다른 테넌트 사람은 보이지 않는다")
		void 남의_테넌트는_보이지_않는다() {
			assertThat(chatPresenceService.getContacts(ME, TENANT))
				.extracting(ChatContactListItem::userId)
				.doesNotContain(OTHER_TENANT_USER);
		}

		@Test
		@DisplayName("접속 상태가 채워진다")
		void 접속_상태가_채워진다() {
			chatPresenceService.sessionConnected(TENANT, PEER, "peer-1");

			assertThat(chatPresenceService.getContacts(ME, TENANT))
				.singleElement()
				.extracting(ChatContactListItem::online)
				.isEqualTo(true);
		}

		@Test
		@DisplayName("접속하지 않은 사람도 목록에는 있다 — 대화는 걸 수 있어야 한다")
		void 오프라인도_목록에_있다() {
			assertThat(chatPresenceService.getContacts(ME, TENANT))
				.singleElement()
				.satisfies(contact -> {
					assertThat(contact.name()).isEqualTo("상대");
					assertThat(contact.department()).isEqualTo("측정2팀");
					assertThat(contact.online()).isFalse();
				});
		}

		@Test
		@DisplayName("email·tel 은 노출하지 않는다 — admin 목록을 재사용하지 않은 이유다")
		void 연락처는_email과_tel을_담지_않는다() {
			assertThat(ChatContactListItem.class.getRecordComponents())
				.extracting(java.lang.reflect.RecordComponent::getName)
				.containsExactly("userId", "name", "department", "role", "online");
		}
	}
}
