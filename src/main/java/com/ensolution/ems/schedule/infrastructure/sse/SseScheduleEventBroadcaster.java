package com.ensolution.ems.schedule.infrastructure.sse;

import com.ensolution.ems.schedule.application.command.event.SheetsSavedEvent;
import com.ensolution.ems.schedule.application.port.out.ScheduleEventBroadcaster;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SSE로 측정계획 편집 이벤트를 전달하는 어댑터. 구독자를 메모리에 들고 있는다.
 * <p>
 * <b>단일 인스턴스 전제.</b> 구독자 레지스트리가 이 프로세스의 힙에만 있으므로 서버를 여러 대로
 * 늘리면 다른 인스턴스에 붙은 사용자에게는 알림이 가지 않는다. 스케일아웃 시점에
 * Redis pub/sub 등으로 인스턴스 간 팬아웃을 붙여야 한다.
 * <p>
 * 키에 {@code tenantId}를 반드시 포함한다. {@code scheduleId}만으로 묶으면 다른 고객사의
 * 편집 알림이 새어 나간다.
 * <p>
 * <b>emitter 쓰기는 블로킹이다.</b> 반쯤 죽은 소켓 하나에서 write가 막히면 그 스레드가 통째로
 * 묶이므로, 팬아웃은 요청 스레드가 아니라 전용 실행기에서 수행한다.
 */
@Slf4j
@Component
public class SseScheduleEventBroadcaster implements ScheduleEventBroadcaster {

	/**
	 * 유휴 연결이 프록시·로드밸런서에서 끊기지 않도록 주석 프레임을 보내는 주기(ms).
	 * nginx의 proxy_read_timeout 기본값(60초)보다 충분히 짧아야 하므로,
	 * 이 값을 늘릴 때는 프록시 설정을 함께 확인한다.
	 */
	private static final long HEARTBEAT_INTERVAL_MS = 15_000L;

	private final Map<StreamKey, List<SseEmitter>> subscribers = new ConcurrentHashMap<>();

	/**
	 * 팬아웃 전용 실행기. 단일 스레드라 같은 구독자에게 가는 이벤트 순서가 보존된다.
	 */
	private final Executor broadcastExecutor;

	private record StreamKey(Long tenantId, Long scheduleId) {}

	public SseScheduleEventBroadcaster() {
		this(Executors.newSingleThreadExecutor(runnable -> {
			Thread thread = new Thread(runnable, "sse-broadcast");
			thread.setDaemon(true);
			return thread;
		}));
	}

	/** 테스트에서 동기 실행기를 주입해 팬아웃을 결정적으로 검증하기 위한 생성자. */
	SseScheduleEventBroadcaster(Executor broadcastExecutor) {
		this.broadcastExecutor = broadcastExecutor;
	}

	@PreDestroy
	void shutdown() {
		if (broadcastExecutor instanceof ExecutorService service) service.shutdownNow();
	}

	@Override
	public void subscribe(Long scheduleId, Long tenantId, SseEmitter emitter) {
		StreamKey key = new StreamKey(tenantId, scheduleId);
		subscribers.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

		// 컨테이너가 연결을 이미 끝낸 뒤의 통보다. 레지스트리에서 빼기만 한다.
		emitter.onCompletion(() -> unregister(key, emitter));
		emitter.onTimeout(() -> unregister(key, emitter));
		emitter.onError(error -> unregister(key, emitter));

		// 첫 프레임을 즉시 보내야 클라이언트가 "열렸다"를 인지한다. 실패하면 이미 끊긴 연결이므로 정리한다.
		if (!send(key, emitter, SseEmitter.event().name("connected").data(scheduleId))) {
			discard(key, emitter);
		}
	}

	@Override
	public void publishSheetsSaved(SheetsSavedEvent event) {
		StreamKey key = new StreamKey(event.tenantId(), event.scheduleId());
		List<SseEmitter> targets = subscribers.get(key);
		if (targets == null || targets.isEmpty()) return;

		// 저장 트랜잭션의 afterCommit(= 요청 스레드)에서 불린다. 여기서 직접 쓰면 죽은 구독 하나가
		// 저장 응답을 붙잡으므로 전송은 전용 실행기에 넘기고 요청 스레드는 곧바로 빠져나간다.
		broadcastExecutor.execute(() -> fanOut(key, event));
	}

	private void fanOut(StreamKey key, SheetsSavedEvent event) {
		// 제출과 실행 사이에 마지막 구독자가 나갔을 수 있으므로 다시 읽는다.
		List<SseEmitter> targets = subscribers.get(key);
		if (targets == null) return;

		for (SseEmitter emitter : targets) {
			boolean sent = send(key, emitter, SseEmitter.event()
				.name("sheets-saved")
				.data(event, MediaType.APPLICATION_JSON));
			if (!sent) discard(key, emitter);
		}
	}

	/**
	 * 살아 있는 연결을 유지한다. 실패한 emitter는 여기서 걸러진다 —
	 * 클라이언트가 브라우저를 강제 종료하면 onError 콜백이 오지 않는 경우가 있어,
	 * 실제로 써 봐야 죽은 연결을 알 수 있다.
	 */
	@Scheduled(fixedDelay = HEARTBEAT_INTERVAL_MS)
	public void sendHeartbeat() {
		subscribers.forEach((key, emitters) -> {
			for (SseEmitter emitter : emitters) {
				if (!send(key, emitter, SseEmitter.event().comment("keep-alive"))) {
					discard(key, emitter);
				}
			}
		});
	}

	/**
	 * 전송 성공 여부를 반환한다. {@link SseEmitter}는 동시 전송에 안전하지 않아
	 * (하트비트와 이벤트 발행이 겹칠 수 있다) emitter 단위로 직렬화한다.
	 */
	private boolean send(StreamKey key, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
		try {
			synchronized (emitter) {
				emitter.send(event);
			}
			return true;
		} catch (IOException | IllegalStateException e) {
			// 끊긴 연결에 쓰는 것은 정상적인 종료 경로다. 스택 없이 남긴다.
			log.debug("[SSE] 전송 실패로 구독을 정리합니다: tenantId={}, scheduleId={}, cause={}",
				key.tenantId(), key.scheduleId(), e.getMessage());
			return false;
		}
	}

	/** 레지스트리에서만 뺀다. 컨테이너가 이미 연결을 끝낸 콜백 경로에서 쓴다. */
	private void unregister(StreamKey key, SseEmitter emitter) {
		// 빈 리스트를 남기면 측정계획 수만큼 키가 쌓인다. 마지막 구독자가 나가면 키째 지운다.
		subscribers.computeIfPresent(key, (ignored, emitters) -> {
			emitters.remove(emitter);
			return emitters.isEmpty() ? null : emitters;
		});
	}

	/**
	 * 레지스트리에서 빼고 비동기 요청도 끝낸다. 전송이 실패한 경로에서 쓴다.
	 * {@code complete()}를 부르지 않으면 클라이언트가 사라진 뒤에도 요청이 타임아웃까지 남아
	 * 서버와 브라우저 양쪽의 커넥션을 붙잡는다.
	 */
	private void discard(StreamKey key, SseEmitter emitter) {
		unregister(key, emitter);
		try {
			emitter.complete();
		} catch (Exception e) {
			// 이미 끊긴 연결을 닫는 것이라 실패해도 더 할 일이 없다.
			log.debug("[SSE] 연결 종료 중 무시 가능한 예외: {}", e.getMessage());
		}
	}
}
