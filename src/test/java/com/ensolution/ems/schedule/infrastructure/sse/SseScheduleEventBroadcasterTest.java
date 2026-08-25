package com.ensolution.ems.schedule.infrastructure.sse;

import com.ensolution.ems.schedule.application.command.event.EditorRef;
import com.ensolution.ems.schedule.application.command.event.SheetsSavedEvent;
import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.domain.sheet.MeasurementCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 편집 알림 전달 규칙 검증.
 * 핵심 계약은 세 가지다 — 같은 측정계획 구독자에게만 가고, 다른 고객사로는 절대 새지 않으며,
 * 끊긴 연결은 스스로 정리된다.
 */
class SseScheduleEventBroadcasterTest {

	private static final Long SCHEDULE_ID = 1L;
	private static final Long TENANT_ID = 100L;

	private SseScheduleEventBroadcaster broadcaster;

	@BeforeEach
	void setUp() {
		// 팬아웃을 호출 스레드에서 그대로 실행해 검증을 결정적으로 만든다.
		broadcaster = new SseScheduleEventBroadcaster(Runnable::run);
	}

	/** 실제 전송 대신 보낸 프레임을 기록한다. 연결이 끊긴 상황도 재현할 수 있다. */
	private static final class RecordingEmitter extends SseEmitter {

		private final List<String> frames = new ArrayList<>();
		private boolean disconnected;
		private int completions;

		private RecordingEmitter() {
			super(30_000L);
		}

		@Override
		public void send(SseEventBuilder builder) throws IOException {
			if (disconnected) throw new IOException("연결이 끊겼습니다.");

			frames.add(builder.build().stream()
				.map(part -> String.valueOf(part.getData()))
				.collect(Collectors.joining()));
		}

		/** 정리 경로가 비동기 요청까지 끝내는지 보려면 complete() 호출을 세야 한다. */
		@Override
		public void complete() {
			completions++;
			super.complete();
		}

		int completions() {
			return completions;
		}

		void disconnect() {
			disconnected = true;
		}

		/** 정리 여부를 확인하려면 다시 살려 두고 "그래도 안 오는지"를 봐야 한다. */
		void reconnect() {
			disconnected = false;
		}

		long countOf(String marker) {
			return frames.stream().filter(frame -> frame.contains(marker)).count();
		}
	}

	private static SheetsSavedEvent event(Long scheduleId, Long tenantId) {
		return new SheetsSavedEvent(
			scheduleId, tenantId,
			new EditorRef("mentee", "김부사수"),
			List.of(MeasurementCategory.GAS),
			ScheduleStatus.MEASURING);
	}

	private RecordingEmitter subscribed(Long scheduleId, Long tenantId) {
		RecordingEmitter emitter = new RecordingEmitter();
		broadcaster.subscribe(scheduleId, tenantId, emitter);
		return emitter;
	}

	@Nested
	@DisplayName("전달 범위")
	class Delivery {

		@Test
		@DisplayName("구독하면 곧바로 연결 확인 프레임을 받는다")
		void sendsConnectedFrameOnSubscribe() {
			assertThat(subscribed(SCHEDULE_ID, TENANT_ID).countOf("connected")).isEqualTo(1);
		}

		@Test
		@DisplayName("같은 측정계획 구독자에게 저장 알림이 간다")
		void deliversToSubscribersOfSameSchedule() {
			RecordingEmitter mentor = subscribed(SCHEDULE_ID, TENANT_ID);
			RecordingEmitter viewer = subscribed(SCHEDULE_ID, TENANT_ID);

			broadcaster.publishSheetsSaved(event(SCHEDULE_ID, TENANT_ID));

			assertThat(mentor.countOf("sheets-saved")).isEqualTo(1);
			assertThat(viewer.countOf("sheets-saved")).isEqualTo(1);
		}

		@Test
		@DisplayName("다른 고객사의 구독자에게는 새지 않는다")
		void neverLeaksAcrossTenants() {
			// scheduleId 는 고객사별로 독립적이라 같은 값이 다른 고객사에도 존재할 수 있다.
			RecordingEmitter mine = subscribed(SCHEDULE_ID, TENANT_ID);
			RecordingEmitter otherTenant = subscribed(SCHEDULE_ID, 200L);

			broadcaster.publishSheetsSaved(event(SCHEDULE_ID, TENANT_ID));

			assertThat(mine.countOf("sheets-saved")).isEqualTo(1);
			assertThat(otherTenant.countOf("sheets-saved")).isZero();
		}

		@Test
		@DisplayName("다른 측정계획 구독자에게는 가지 않는다")
		void doesNotDeliverToOtherSchedules() {
			RecordingEmitter otherSchedule = subscribed(2L, TENANT_ID);

			broadcaster.publishSheetsSaved(event(SCHEDULE_ID, TENANT_ID));

			assertThat(otherSchedule.countOf("sheets-saved")).isZero();
		}

		@Test
		@DisplayName("구독자가 없어도 발행은 조용히 지나간다")
		void publishesWithoutSubscribers() {
			broadcaster.publishSheetsSaved(event(SCHEDULE_ID, TENANT_ID));
		}
	}

	@Nested
	@DisplayName("끊긴 연결 정리")
	class Cleanup {

		@Test
		@DisplayName("전송에 실패한 구독자는 정리되어 다음 발행에서 빠진다")
		void dropsSubscriberOnSendFailure() {
			RecordingEmitter dead = subscribed(SCHEDULE_ID, TENANT_ID);
			dead.disconnect();

			broadcaster.publishSheetsSaved(event(SCHEDULE_ID, TENANT_ID));
			dead.reconnect();
			broadcaster.publishSheetsSaved(event(SCHEDULE_ID, TENANT_ID));

			assertThat(dead.countOf("sheets-saved")).isZero();
		}

		@Test
		@DisplayName("하트비트가 죽은 연결을 걸러낸다 — 브라우저 강제 종료는 콜백이 오지 않는다")
		void dropsSubscriberOnHeartbeatFailure() {
			RecordingEmitter alive = subscribed(SCHEDULE_ID, TENANT_ID);
			RecordingEmitter dead = subscribed(SCHEDULE_ID, TENANT_ID);
			dead.disconnect();

			broadcaster.sendHeartbeat();
			dead.reconnect();
			broadcaster.publishSheetsSaved(event(SCHEDULE_ID, TENANT_ID));

			assertThat(alive.countOf("keep-alive")).isEqualTo(1);
			assertThat(alive.countOf("sheets-saved")).isEqualTo(1);
			assertThat(dead.countOf("sheets-saved")).isZero();
		}

		@Test
		@DisplayName("정리된 구독은 비동기 요청까지 종료된다 — 안 그러면 타임아웃까지 커넥션이 남는다")
		void completesEmitterOnDiscard() {
			RecordingEmitter dead = subscribed(SCHEDULE_ID, TENANT_ID);
			dead.disconnect();

			broadcaster.sendHeartbeat();

			assertThat(dead.completions()).isEqualTo(1);
		}

		@Test
		@DisplayName("살아 있는 구독은 종료하지 않는다")
		void keepsHealthySubscriptionOpen() {
			RecordingEmitter alive = subscribed(SCHEDULE_ID, TENANT_ID);

			broadcaster.sendHeartbeat();
			broadcaster.publishSheetsSaved(event(SCHEDULE_ID, TENANT_ID));

			assertThat(alive.completions()).isZero();
		}

		@Test
		@DisplayName("한 구독자가 끊겨도 나머지에게는 그대로 전달된다")
		void keepsDeliveringToHealthySubscribers() {
			RecordingEmitter dead = subscribed(SCHEDULE_ID, TENANT_ID);
			RecordingEmitter alive = subscribed(SCHEDULE_ID, TENANT_ID);
			dead.disconnect();

			broadcaster.publishSheetsSaved(event(SCHEDULE_ID, TENANT_ID));

			assertThat(alive.countOf("sheets-saved")).isEqualTo(1);
		}
	}
}
